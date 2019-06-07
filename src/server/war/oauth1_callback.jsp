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
        function callback(verifier) {
            if (typeof window.opener !== 'undefined') {
                if (typeof window.opener.oauth1callback === 'function') {
                    window.opener.oauth1callback(verifier);
                }
            }
        }
    </script>
    <%
        String error = request.getParameter("error");
        String verifier = request.getParameter("oauth_verifier");
        if (error != null || verifier == null) {
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
                callback('<%=verifier%>');
            </script>
            <%
        }
    %>
</body>
</html>