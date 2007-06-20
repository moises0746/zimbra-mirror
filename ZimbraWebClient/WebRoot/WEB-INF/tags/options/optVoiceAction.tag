<%@ tag body-content="empty" %>
<%@ attribute name="accountindex" rtexprvalue="true" required="true" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<%@ taglib prefix="zm" uri="com.zimbra.zm" %>
<%@ taglib prefix="app" uri="com.zimbra.htmlclient" %>

<app:handleError>
</app:handleError>

<zm:modifyVoicePrefs var="result" phone="${param.phone}"
    emailnotificationactive="${param.emailNotificationActive}" emailnotificationaddress="${param.emailNotificationAddress}"
    callforwardingactive="${param.callForwardingAllActive}" callforwardingforwardto="${param.callForwardingAllNumber}"
/>


<c:choose>
    <c:when test="${result}">
        <app:status><fmt:message key="optionsSaved"/></app:status>
    </c:when>
    <c:otherwise>
        <app:status><fmt:message key="noOptionsChanged"/></app:status>
    </c:otherwise>
</c:choose>    


