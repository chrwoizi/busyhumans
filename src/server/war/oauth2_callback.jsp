<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<html>
<head>
    <meta http-equiv="content-type" content="text/html; charset=UTF-8">
    <title>BUSY HUMANS</title>
    <link href="/favicon.png" rel="shortcut icon" type="image/png"/>
    <link href="/favicon.png" rel="icon" type="image/png">
</head>
<body>
    <script type="text/javascript">
        function callback(code) {
            if (typeof window.opener !== 'undefined') {
                if (typeof window.opener.oauth2callback === 'function') {
                    window.opener.oauth2callback(code);
                }
            }
        }
    </script>
    <%
        String error = request.getParameter("error");
        String code = request.getParameter("code");
        if (error != null || code == null) {
            %>
            <script type="text/javascript">
                window.close();
                callback();
            </script>
            <%
        } else {
            %>
            <script type="text/javascript">
                window.close();
                callback('<%=code%>');
            </script>
            <%
        }
    %>
</body>
</html>