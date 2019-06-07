<%@ page import="com.c5000.mastery.shared.PublicFacebookConfig" %>
<%@ page import="java.util.List" %>
<%@ page import="java.util.ArrayList" %>
<%@ page import="com.c5000.mastery.shared.Config" %>
<%@ page import="java.net.URLDecoder" %>
<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<html>
<head prefix="og: http://ogp.me/ns# fb: http://ogp.me/ns/fb# busyhumans: http://ogp.me/ns/fb/busyhumans#">
    <meta http-equiv="content-type" content="text/html; charset=UTF-8">
    <title>BUSY HUMANS</title>
    <link href="/favicon.png" rel="shortcut icon" type="image/png"/>
    <link href="/favicon.png" rel="icon" type="image/png">
    <%
        String url = request.getRequestURL().toString().replace("meta.jsp", "index.html");
        String queryString = request.getQueryString();
        if (queryString != null) {
            if(url.startsWith(Config.BASE_URL_DEV)) {
                url += "?" + Config.GWT_CODE_SVR;
            }
            if(queryString.startsWith("token=")) {
                String token = queryString.substring("token=".length());
                int nextParam = token.indexOf("&");
                if(nextParam >= 0)
                    token = token.substring(0, nextParam);
                url += "#" + URLDecoder.decode(token, "UTF-8");
            }

            List params = new ArrayList();
            params.add("title");
            params.add("description");
            params.add("og:title");
            params.add("og:description");
            params.add("og:type");
            params.add("og:url");
            params.add("og:image");
            params.add("og:site_name");

            for(int i = 0; i < params.size(); ++i) {
                String param = (String)params.get(i);
                String value = (String) request.getAttribute(param);
                if (value != null) {
                    %>
                    <meta property="<%=param%>" content="<%=value%>">
                    <%
                }
            }
        }
    %>
    <meta property="fb:app_id" content="<%=PublicFacebookConfig.FACEBOOK_APP_ID%>">
    <meta http-equiv="refresh" content="0; URL=<%=url%>">
</head>
<body>
    redirecting to <a href="<%=url%>">BUSY HUMANS</a>...
</body>
</html>