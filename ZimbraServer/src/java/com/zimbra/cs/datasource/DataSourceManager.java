/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2006, 2007, 2008, 2009, 2010, 2011, 2012, 2013 Zimbra Software, LLC.
 * 
 * The contents of this file are subject to the Zimbra Public License
 * Version 1.4 ("License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 * http://www.zimbra.com/license.
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied.
 * ***** END LICENSE BLOCK *****
 */
package com.zimbra.cs.datasource;

import com.zimbra.soap.admin.type.DataSourceType;
import com.zimbra.common.account.ZAttrProvisioning.AccountStatus;
import com.zimbra.common.localconfig.LC;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.DateUtil;
import com.zimbra.common.util.Pair;
import com.zimbra.common.util.StringUtil;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.DataSource;
import com.zimbra.cs.account.DataSource.DataImport;
import com.zimbra.cs.account.DataSourceConfig;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.datasource.imap.ImapSync;
import com.zimbra.cs.db.DbPool;
import com.zimbra.cs.db.DbPool.DbConnection;
import com.zimbra.cs.db.DbScheduledTask;
import com.zimbra.cs.extension.ExtensionUtil;
import com.zimbra.cs.gal.GalImport;
import com.zimbra.cs.mailbox.Folder;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.MailboxManager;
import com.zimbra.cs.mailbox.ScheduledTaskManager;
import com.zimbra.cs.util.Zimbra;

import java.io.File;
import java.lang.reflect.Constructor;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;

import static com.zimbra.common.util.TaskUtil.newDaemonThreadFactory;
import static java.util.Collections.newSetFromMap;
import static java.util.concurrent.Executors.newCachedThreadPool;

public class DataSourceManager {

    private static DataSourceManager sInstance;

    // accountId -> dataSourceId -> ImportStatus
    private static final Map<String, Map<String, ImportStatus>> sImportStatus =
        new HashMap<String, Map<String, ImportStatus>>();

    // Bug: 40799
    // Methods to keep track of managed data sources so we can easily detect
    // when a data source has been removed while syncing
    private static final Set<Object> sManagedDataSources = newConcurrentHashSet();

    private final DataSourceConfig config;

    private static final ExecutorService executor = newCachedThreadPool(newDaemonThreadFactory("ImportData"));

    private static <E> Set<E> newConcurrentHashSet() {
        return newSetFromMap(new ConcurrentHashMap<E, Boolean>());
    }

    private static Object key(String accountId, String dataSourceId) {
        return new Pair<String, String>(accountId, dataSourceId);
    }

    public static void addManaged(DataSource ds) {
        sManagedDataSources.add(key(ds.getAccountId(), ds.getId()));
    }

    public static void deleteManaged(String accountId, String dataSourceId) {
        sManagedDataSources.remove(key(accountId, dataSourceId));
    }

    public static boolean isManaged(DataSource ds) {
        return sManagedDataSources.contains(key(ds.getAccountId(), ds.getId()));
    }

    public DataSourceManager() {
        this.config = loadConfig();
    }

    private DataSourceConfig loadConfig() {
        try {
            File file = new File(LC.data_source_config.value());
            DataSourceConfig config = DataSourceConfig.read(file);
            ZimbraLog.datasource.debug("Loaded datasource configuration from '%s'", file);

            for (DataSourceConfig.Service service : config.getServices()) {
                ZimbraLog.datasource.debug(
                        "Loaded %d folder mappings for service '%s'",
                        service.getFolders().size(), service.getName());
            }
            return config;
        }
        catch (Exception e) {
            Zimbra.halt("Unable to load datasource config", e);
            return null;
        }
    }

    /**
     * @param ds not used
     * @param folder not used
     */
    public boolean isSyncCapable(DataSource ds, Folder folder) {
        return true;
    }

    /**
     * @param ds not used
     * @param folder not used
     */
    public boolean isSyncEnabled(DataSource ds, Folder folder) {
        return true;
    }

    public synchronized static DataSourceManager getInstance() {
        if (sInstance == null) {
            String className = LC.zimbra_class_datasourcemanager.value();
            if (!StringUtil.isNullOrEmpty(className)) {
                try {
                    try {
                        sInstance = (DataSourceManager) Class.forName(className).newInstance();
                    } catch (ClassNotFoundException cnfe) {
                        // ignore and look in extensions
                        sInstance = (DataSourceManager) ExtensionUtil.findClass(className).newInstance();
                    }
                } catch (Exception e) {
                    ZimbraLog.system.error("Unable to initialize %s.", className, e);
                }
            }
            if (sInstance == null) {
                sInstance = new DataSourceManager();
                ZimbraLog.datasource.info("Initialized %s.", sInstance.getClass().getName());
            }
        }

        return sInstance;
    }

    public static DataSourceConfig getConfig() {
        return getInstance().config;
    }

    public Mailbox getMailbox(DataSource ds) throws ServiceException {
        return MailboxManager.getInstance().getMailboxByAccount(ds.getAccount());
    }

    public DataImport getDataImport(DataSource ds) throws ServiceException {
        switch (ds.getType()) {
        case pop3:
            return new Pop3Sync(ds);
        case imap:
            return new ImapSync(ds);
        case caldav:
            return new CalDavDataImport(ds);
        case rss:
        case cal:
            return new RssImport(ds);
        case gal:
            return new GalImport(ds);
        case xsync:
            try {
                String className = LC.data_source_xsync_class.value();
                if (className != null && className.length() > 0) {
                    Class<?> cmdClass = null;
                    try {
                        cmdClass = Class.forName(className);
                    } catch (ClassNotFoundException x) {
                        cmdClass = ExtensionUtil.findClass(className);
                    }
                    Constructor<?> constructor = cmdClass.getConstructor(new Class[] {DataSource.class});
                    return (DataImport) constructor.newInstance(ds);
                }
            } catch (Exception x) {
                ZimbraLog.datasource.warn("Failed instantiating xsync class: %s", ds, x);
            }
        default:
            // yab is handled by OfflineDataSourceManager
            throw new IllegalArgumentException("Unknown data import type: " + ds.getType());
        }
    }

    public static String getDefaultImportClass(DataSourceType ds) {
        switch (ds) {
        case caldav:
            return CalDavDataImport.class.getName();
        case gal:
            return GalImport.class.getName();
        }
        return null;
    }

    /*
     * Tests connecting to a data source.  Do not actually create the
     * data source.
     */
    public static void test(DataSource ds) throws ServiceException {
        ZimbraLog.datasource.info("Testing: %s", ds);
        try {
            DataImport di = getInstance().getDataImport(ds);
            di.test();
            ZimbraLog.datasource.info("Test succeeded: %s", ds);
        } catch (ServiceException x) {
            ZimbraLog.datasource.warn("Test failed: %s", ds, x);
            throw x;
        }
    }

    public static List<ImportStatus> getImportStatus(Account account)
        throws ServiceException {
        List<DataSource> dsList = Provisioning.getInstance().getAllDataSources(account);
        List<ImportStatus> allStatus = new ArrayList<ImportStatus>();
        for (DataSource ds : dsList) {
            allStatus.add(getImportStatus(account, ds));
        }
        return allStatus;
    }

    public static ImportStatus getImportStatus(Account account, DataSource ds) {
        ImportStatus importStatus;

        synchronized (sImportStatus) {
            Map<String, ImportStatus> isMap = sImportStatus.get(account.getId());
            if (isMap == null) {
                isMap = new HashMap<String, ImportStatus>();
                sImportStatus.put(account.getId(), isMap);
            }
            importStatus = isMap.get(ds.getId());
            if (importStatus == null) {
                importStatus = new ImportStatus(ds.getId());
                isMap.put(ds.getId(), importStatus);
            }
        }

        return importStatus;
    }

    public static void asyncImportData(final DataSource ds) {
        ZimbraLog.datasource.debug("Requesting async import for DataSource %s", ds.getId());

        executor.submit(new Runnable() {
            @Override
            public void run() {
                try {
                    // todo exploit comonality with DataSourceTask
                    ZimbraLog.clearContext();
                    ZimbraLog.addMboxToContext(ds.getMailbox().getId());
                    ZimbraLog.addAccountNameToContext(ds.getAccount().getName());
                    ZimbraLog.addDataSourceNameToContext(ds.getName());
                    ZimbraLog.datasource.debug("Running on-demand import for DataSource %s", ds.getId());

                    DataSourceManager.importData(ds);

                } catch (Exception e) {
                    ZimbraLog.datasource.warn("On-demand DataSource import failed.", e);
                }
                finally {
                    ZimbraLog.clearContext();
                }
            }
        });
    }

    public static void importData(DataSource ds) throws ServiceException {
        importData(ds, null, true);
    }

    public static void importData(DataSource fs, boolean fullSync)
        throws ServiceException {
        importData(fs, null, fullSync);
    }


    /**
     * Executes the data source's {@link MailItemImport} implementation to import data in the current thread.
     */
    public static void importData(DataSource ds, List<Integer> folderIds, boolean fullSync) throws ServiceException {

        ZimbraLog.datasource.info("Requested import.");
        AccountStatus status = ds.getAccount().getAccountStatus();
        if (!(status.isActive() || status.isLocked() || status.isLockout())) {
            ZimbraLog.datasource.info("Account is not active. Skipping import.");
            return;
        }
        if (DataSourceManager.getInstance().getMailbox(ds).getMaintenance() != null) {
            ZimbraLog.datasource.info("Mailbox is in maintenance mode. Skipping import.");
            return;
        }
        ImportStatus importStatus = getImportStatus(ds.getAccount(), ds);
        synchronized (importStatus) {
            if (importStatus.isRunning()) {
                ZimbraLog.datasource.info("Attempted to start import while " +
                    " an import process was already running.  Ignoring the second request.");
                return;
            }
            importStatus.mHasRun = true;
            importStatus.mIsRunning = true;
            importStatus.mSuccess = false;
            importStatus.mError = null;
        }

        boolean success = false;
        String error = null;

        addManaged(ds);

        try {
            ZimbraLog.datasource.info("Importing data for data source '%s'", ds.getName());
            getInstance().getDataImport(ds).importData(folderIds, fullSync);
            success = true;
            resetErrorStatus(ds);
        } catch (ServiceException x) {
            error = generateErrorMessage(x);
            setErrorStatus(ds, error);
            throw x;
        } finally {
            ZimbraLog.datasource.info("Import completed for data source '%s'", ds.getName());
            synchronized (importStatus) {
                importStatus.mSuccess = success;
                importStatus.mError = error;
                importStatus.mIsRunning = false;
            }
        }
    }

    public static void resetErrorStatus(DataSource ds) {
        if (ds.getAttr(Provisioning.A_zimbraDataSourceFailingSince) != null ||
            ds.getAttr(Provisioning.A_zimbraDataSourceLastError) != null) {
            Map<String, Object> attrs = new HashMap<String, Object>();
            attrs.put(Provisioning.A_zimbraDataSourceFailingSince, null);
            attrs.put(Provisioning.A_zimbraDataSourceLastError, null);
            try {
                Provisioning.getInstance().modifyAttrs(ds, attrs);
            } catch (ServiceException e) {
                ZimbraLog.datasource.warn("Unable to reset error status for data source %s.", ds.getName());
            }
        }
    }

    private static void setErrorStatus(DataSource ds, String error) {
        Map<String, Object> attrs = new HashMap<String, Object>();
        attrs.put(Provisioning.A_zimbraDataSourceLastError, error);
        if (ds.getAttr(Provisioning.A_zimbraDataSourceFailingSince) == null) {
            attrs.put(Provisioning.A_zimbraDataSourceFailingSince, DateUtil.toGeneralizedTime(new Date()));
        }
        try {
            Provisioning.getInstance().modifyDataSource(ds.getAccount(), ds.getId(), attrs);
        } catch (ServiceException e) {
            ZimbraLog.datasource.warn("Unable to set error status for data source %s.", ds.getName());
        }
    }

    private static String generateErrorMessage(Throwable t) {
        StringBuilder buf = new StringBuilder();
        while (t != null) {
            // HACK: go with JavaMail error message
            if (t.getClass().getName().startsWith("javax.mail.")) {
                String msg = t.getMessage();
                return msg != null ? msg : t.toString();
            }
            if (buf.length() > 0) {
                buf.append(", ");
            }
            String msg = t.getMessage();
            buf.append(msg != null ? msg : t.toString());
            t = t.getCause();
        }
        return buf.toString();
    }

    static void cancelTask(Mailbox mbox, String dsId)
        throws ServiceException {
        ScheduledTaskManager.cancel(DataSourceTask.class.getName(), dsId, mbox.getId(), false);
        DbScheduledTask.deleteTask(DataSourceTask.class.getName(), dsId);
    }

    public static DataSourceTask getTask(Mailbox mbox, String dsId) {
        return (DataSourceTask) ScheduledTaskManager.getTask(DataSourceTask.class.getName(), dsId, mbox.getId());
    }

    /**
     * Cancels scheduling for this <tt>DataSource</tt>
     *
     * @param account
     * @param dsId
     * @throws ServiceException
     */
    public static void cancelSchedule(Account account, String dsId)
    throws ServiceException {
        updateSchedule(account, null, dsId, true);
    }

    /**
     * Cancels scheduling for this <tt>DataSource</tt>
     *
     * @param account Account for the DataSource, cannot be null
     * @param ds The DataSource, cannot be null
     * @throws ServiceException
     */
    public static void updateSchedule(Account account, DataSource ds)
    throws ServiceException {
        updateSchedule(account, ds, ds.getId(), false);
    }

    /**
     *
     * Updates scheduling data for this <tt>DataSource</tt> both in memory and in the
     * <tt>data_source_task</tt> database table.
     *
     * @param account Account for the DataSource, cannot be null.
     * @param ds The DataSource.  Ignored if cancelSchedule is true.
     * @param dsId zimbraId of the DataSource.
     * @param cancelSchedule cancel scheduling for the DataSource.
     * @throws ServiceException
     */
    private static void updateSchedule(Account account, DataSource ds, String dsId, boolean cancelSchedule)
    throws ServiceException {
        if (!LC.data_source_scheduling_enabled.booleanValue()) {
            return;
        }
        String accountId = account.getId();
        ZimbraLog.datasource.debug("Updating schedule for account %s, data source %s", accountId, dsId);

        int mboxId = MailboxManager.getInstance().lookupMailboxId(account.getId());
        if (mboxId == -1)
            return;

        if (cancelSchedule) {
            ZimbraLog.datasource.info(
                "Data source %s was deleted.  Deleting scheduled task.", dsId);
            ScheduledTaskManager.cancel(DataSourceTask.class.getName(), dsId, mboxId, false);
            DbScheduledTask.deleteTask(DataSourceTask.class.getName(), dsId);
            deleteManaged(accountId, dsId);
            return;
        }
        if (!ds.isEnabled()) {
            ZimbraLog.datasource.info(
                "Data source %s is disabled.  Deleting scheduled task.", dsId);
            ScheduledTaskManager.cancel(DataSourceTask.class.getName(), dsId, mboxId, false);
            DbScheduledTask.deleteTask(DataSourceTask.class.getName(), dsId);
            return;
        }

        ZimbraLog.datasource.info("Updating schedule for data source %s", ds.getName());
        DbConnection conn = null;
        try {
            conn = DbPool.getConnection();
            ScheduledTaskManager.cancel(conn, DataSourceTask.class.getName(), ds.getId(), mboxId, false);
            if (ds.isScheduled()) {
                DataSourceTask task = new DataSourceTask(mboxId, accountId, dsId, ds.getPollingInterval());
                ZimbraLog.datasource.debug("Scheduling %s", task);
                ScheduledTaskManager.schedule(conn, task);
            }
            conn.commit();
        } catch (ServiceException e) {
            ZimbraLog.datasource.warn("Unable to schedule data source %s", ds.getName(), e);
            DbPool.quietRollback(conn);
        } finally {
            DbPool.quietClose(conn);
        }
    }
}
