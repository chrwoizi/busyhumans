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
        function callback(status, id, code) {
            if (typeof window.parent !== 'undefined') {
                if (typeof window.top.ytuploadcallback === 'function') {
                    window.top.ytuploadcallback(status, id, code);
                }
            }
        }
    </script>
    <%
        String status = request.getParameter("status");
        String id = request.getParameter("id");
        String code = request.getParameter("code");
        if (status != null) {
            %>
            <script type="text/javascript">
                callback('<%=status%>', '<%=id%>', '<%=code%>');
            </script>
            <%
        }
    %>
</body>
</html>