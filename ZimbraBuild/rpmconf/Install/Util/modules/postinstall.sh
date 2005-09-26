#!/bin/bash
# 
# ***** BEGIN LICENSE BLOCK *****
# Version: ZPL 1.1
# 
# The contents of this file are subject to the Zimbra Public License
# Version 1.1 ("License"); you may not use this file except in
# compliance with the License. You may obtain a copy of the License at
# http://www.zimbra.com/license
# 
# Software distributed under the License is distributed on an "AS IS"
# basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See
# the License for the specific language governing rights and limitations
# under the License.
# 
# The Original Code is: Zimbra Collaboration Suite.
# 
# The Initial Developer of the Original Code is Zimbra, Inc.
# Portions created by Zimbra are Copyright (C) 2005 Zimbra, Inc.
# All Rights Reserved.
# 
# Contributor(s):
# 
# ***** END LICENSE BLOCK *****
# 

postInstallConfig() {
	echo ""
	echo "Post installation configuration"
	echo ""

	chmod 755 /opt/zimbra

	if [ $UPGRADE = "yes" ]; then
		#restore old config, then overwrite...
		restoreExistingConfig
	fi

	if [ $UPGRADE = "no" -a $STORE_HERE = "yes" ]; then
		echo -n "Creating db..."
		runAsZimbra "/opt/zimbra/libexec/zmmyinit"
		echo "done"
	fi

	if [ $UPGRADE = "no" -a $LOGGER_HERE = "yes" ]; then
		echo -n "Creating logger db..."
		runAsZimbra "/opt/zimbra/libexec/zmloggerinit"
		echo "done"
	fi

	echo -n "Setting the hostname to $HOSTNAME..."
	runAsZimbra "zmlocalconfig -e zimbra_server_hostname=${HOSTNAME}"
	echo "done"

	echo -n "Setting the LDAP host to $LDAPHOST..."
	runAsZimbra "zmlocalconfig -e ldap_host=$LDAPHOST"
	runAsZimbra "zmlocalconfig -e ldap_port=$LDAPPORT"
	echo "done"

	SERVERCREATED="no"
	if [ $UPGRADE = "no" ]; then
		if [ $LDAP_HERE = "yes" ]; then
			echo -n "Initializing ldap..."
			runAsZimbra "/opt/zimbra/libexec/zmldapinit"
			echo "done"
		else
			# set the ldap password in localconfig only
			echo -n "Setting the ldap passwords..."
			runAsZimbra "zmlocalconfig -f -e ldap_root_password=$LDAPROOTPW"
			runAsZimbra "zmlocalconfig -f -e zimbra_ldap_password=$LDAPZIMBRAPW"
			echo "done"
		fi

		echo -n "Creating server $HOSTNAME..."
		runAsZimbra "zmprov cs $HOSTNAME"
		if [ $? = 0 ]; then
			SERVERCREATED="yes"
		fi
		echo "done"

		if [ x$CREATEDOMAIN != "x" ]; then
			echo -n "Creating domain $CREATEDOMAIN..."
			runAsZimbra "zmprov cd $CREATEDOMAIN"
			runAsZimbra "zmprov mcf zimbraDefaultDomainName $CREATEDOMAIN"
			echo "done"
			if [ x$CREATEADMIN != "x" ]; then
				echo -n "Creating admin account $CREATEADMIN..."
				runAsZimbra "zmprov ca $CREATEADMIN $CREATEADMINPASS zimbraIsAdminAccount TRUE"
				LOCALHOSTNAME=`hostname --fqdn`
				if [ $LOCALHOSTNAME = $CREATEDOMAIN ]; then
					runAsZimbra "zmprov aaa $CREATEADMIN postmaster@$HOSTNAME"
				fi
				echo "done"
			fi
		fi
	else
		if [ $LDAP_HERE = "yes" ]; then
			echo -n "Starting ldap..."
			runAsZimbra "ldap start"
			runAsZimbra "zmldapapplyldif"
			echo "done"
		fi
	fi

	if [ $LDAP_HERE = "yes" ]; then
		SERVICES="zimbraServiceInstalled ldap"
	fi

	if [ $LOGGER_HERE = "yes" ]; then
		SERVICES="$SERVICES zimbraServiceInstalled logger"
		runAsZimbra "zmprov mcf zimbraLogHostname $HOSTNAME"
	fi

	if [ $STORE_HERE = "yes" ]; then
		if [ $SERVERCREATED = "yes" ]; then
			echo -n "Setting smtp host to $SMTPHOST..."
			runAsZimbra "zmprov ms $HOSTNAME zimbraSmtpHostname $SMTPHOST"
			echo "done"
		fi

		echo -n "Adding $HOSTNAME to zimbraMailHostPool in default COS..."
		runAsZimbra "id=\`zmprov gs $HOSTNAME | grep zimbraId | awk '{print \$2}'\`; for i in \`zmprov gc default | grep zimbraMailHostPool | sed 's/zimbraMailHostPool: //'\`; do host=\"\$host zimbraMailHostPool \$i\"; done; zmprov mc default \$host zimbraMailHostPool \$id"
		echo "done"

		SERVICES="$SERVICES zimbraServiceInstalled mailbox"
	fi

	if [ $POSTFIX_HERE = "yes" ]; then
		echo -n "Initializing mta config..."
		runAsZimbra "/opt/zimbra/libexec/zmmtainit $LDAPHOST"
		echo "done"

		# zmprov isn't very friendly

		SERVICES="$SERVICES zimbraServiceInstalled mta"

		if [ $RUNAV = "yes" ]; then
			SERVICES="$SERVICES zimbraServiceInstalled antivirus"
			runAsZimbra "zmlocalconfig -e av_notify_user=$AVUSER"
			runAsZimbra "zmlocalconfig -e av_notify_domain=$AVDOMAIN"
		fi
		if [ $RUNSA = "yes" ]; then
			SERVICES="$SERVICES zimbraServiceInstalled antispam"
		fi
	fi

	if [ $SNMP_HERE = "yes" ]; then
		echo -n "Configuring SNMP..."
		runAsZimbra "zmlocalconfig -e snmp_notify=$SNMPNOTIFY"
		runAsZimbra "zmlocalconfig -e smtp_notify=$SMTPNOTIFY"
		runAsZimbra \
			"zmlocalconfig -e snmp_trap_host=$SNMPTRAPHOST"
		runAsZimbra "zmlocalconfig -e smtp_source=$SMTPSOURCE"
		runAsZimbra \
			"zmlocalconfig -e smtp_destination=$SMTPDEST"
		runAsZimbra "zmsnmpinit"
		echo "done"
		SERVICES="$SERVICES zimbraServiceInstalled snmp"
	fi

	echo -n "Setting services on $HOSTNAME..."
	runAsZimbra "zmprov ms $HOSTNAME $SERVICES"

	ENABLEDSERVICES=`echo $SERVICES | sed -e 's/zimbraServiceInstalled/zimbraServiceEnabled/g'`
	runAsZimbra "zmprov ms $HOSTNAME $ENABLEDSERVICES"

	LOCALSERVICES=`echo $SERVICES | sed -e 's/zimbraServiceInstalled //g'`
	runAsZimbra "zmlocalconfig -e zimbra_services=\"$LOCALSERVICES\""
	echo "done"

	if [ $STORE_HERE = "yes" -o $POSTFIX_HERE = "yes" ]; then
		echo -n "Setting up SSL..."
		runAsZimbra "zmcreatecert"
		if [ $STORE_HERE = "yes" ]; then
			runAsZimbra "zmcertinstall mailbox"
			runAsZimbra "zmtlsctl $MODE"
		fi
		if [ $POSTFIX_HERE = "yes" ]; then
			runAsZimbra "zmcertinstall mta /opt/zimbra/ssl/ssl/server/smtpd.crt /opt/zimbra/ssl/ssl/ca/ca.key"
		fi

		runAsZimbra "zmlocalconfig -e ssl_allow_untrusted_certs=$ALLOWSELFSIGNED"
		echo "done"
		if [ $UPGRADE = "yes" ]; then
			restoreCerts
		fi
	fi

	setupCrontab
}
