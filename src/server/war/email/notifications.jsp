<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN">
<html>

<head>
    <%@ page import="com.c5000.mastery.backend.services.NotificationS$" %>
    <%@ page import="java.util.List" %>
    <%@ page import="java.util.UUID" %>
    <%@ page import="com.c5000.mastery.shared.Config" %>
    <meta http-equiv="content-type" content="text/html; charset=UTF-8">
    <title>BUSY HUMANS</title>
</head>

<body style="margin: 0; padding: 0; background-color: #E0E0E0;">

    <%
        String accountIdStr = request.getParameter("accountId");
        if (accountIdStr != null) {
            try {
                UUID accountId = UUID.fromString(accountIdStr);
                List notifications = NotificationS$.MODULE$.getNotifications(accountId);
                if (notifications != null) {
    %>
    <table width="100%" border="0" cellspacing="0" cellpadding="0">
        <tbody>
            <tr>
                <td style="padding: 20px;" align="center" valign="bottom">
                    <table style="border-radius: 5px; box-shadow: 0 0 4px rgba(0, 0, 0, 0.5);" width="550" border="0" cellspacing="0" cellpadding="0">
                        <tbody>
                            <tr>
                                <td style="height: 36px; border-top-left-radius: 5px; border-top-right-radius: 5px; background-color: #000000; font-size: 0;">
                                    <a href="http://busyhumans.com" style="font-size: 0;">
                                        <img src="http://busyhumans.com/static/topbar-logo.png" alt="Busy Humans">
                                    </a>

                                    <div style="display: inline-block; height: 36px;"/>
                                </td>
                            </tr>
                            <tr>
                                <td style="padding: 10px 28px 28px 28px; background-color: #FFFFFF;">
                                    <h1 style="margin: 8px 0; font-family: Arial; font-size: 10pt; font-weight: bold;">
                                        Your recent notifications on Busy Humans:
                                    </h1>
                                    <ul>
                                        <% for (int i = 0; i < notifications.size(); ++i) { %>
                                        <li style="margin-top: 10px; margin-bottom: 10px; font-family: Arial; font-size: 10pt;">
                                            <%=(String) notifications.get(i)%>
                                        </li>
                                        <% } %>
                                    </ul>
                                </td>
                            </tr>
                            <tr>
                                <td style="height: 50px; padding: 10px; border-top: 1px solid #CCCCCC; border-bottom-left-radius: 5px; border-bottom-right-radius: 5px; background-color: #E8E8E8; font-family: Arial; font-size: 8pt; color: #999999;">
                                    You received this email because your email address has been registered with a user
                                    account at
                                    <a href="<%=Config.BASE_URL_GWT%>" style="font-family: Arial; font-size: 8pt; color: #999999;">busyhumans.com</a>.
                                    The extent of these notifications can be changed on your
                                    <a href="<%=Config.BASE_URL_GWT%>#preferences" style="font-family: Arial; font-size: 8pt; color: #999999;">preferences page</a>.
                                    Please contact us at
                                    <a href="mailto:info@busyhumans.com" style="font-family: Arial; font-size: 8pt; color: #999999;">info@busyhumans.com</a>
                                    if you think this email was sent by mistake and/or if you want your email address to
                                    be removed from our service.
                                </td>
                            </tr>
                        </tbody>
                    </table>
                </td>
            </tr>
        </tbody>
    </table>
    <%
                }
            } catch (IllegalArgumentException ex) {
            }
        }
    %>

</body>

</html>
