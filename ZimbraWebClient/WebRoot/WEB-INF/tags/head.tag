<%@ tag body-content="scriptless" %>
<%@ attribute name="title" rtexprvalue="true" required="false" %>
<%@ taglib prefix="zm" uri="com.zimbra.zm" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>

<head>
    <title>
        <c:if test="${empty title}"><fmt:message key="zimbraTitle"/></c:if>
        <c:if test="${!empty title}"><fmt:message key="zimbraTitle"/>: ${fn:escapeXml(title)}</c:if>
    </title>
    <zm:getMailbox var="mailbox"/>
    <c:set var="skin" value="${empty mailbox.prefs.skin ? 'sand' : mailbox.prefs.skin}"/>
    <!-- skin is ${skin} -->
    <style type="text/css">
       @import url( "<c:url value='/css/common,login,zhtml,${skin},skin.css?skin=${skin}'/>" );
    </style>
    <link rel="ICON" type="image/gif" href="<c:url value='/img/loRes/logo/favicon.gif'/>">
    <link rel="SHORTCUT ICON" href="<c:url value='/img/loRes/logo/favicon.ico'/>">
    <jsp:doBody/>
</head>
