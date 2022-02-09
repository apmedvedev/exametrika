/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.server.web;

import java.io.File;
import java.net.InetAddress;
import java.net.UnknownHostException;

import javax.servlet.ServletException;

import org.apache.catalina.Context;
import org.apache.catalina.Lifecycle;
import org.apache.catalina.LifecycleEvent;
import org.apache.catalina.LifecycleException;
import org.apache.catalina.LifecycleListener;
import org.apache.catalina.Wrapper;
import org.apache.catalina.connector.Connector;
import org.apache.catalina.startup.Tomcat;

import com.exametrika.api.server.IServerService;
import com.exametrika.api.server.web.config.WebServerConfiguration;
import com.exametrika.common.config.ILoadContext;
import com.exametrika.common.l10n.DefaultMessage;
import com.exametrika.common.l10n.ILocalizedMessage;
import com.exametrika.common.l10n.Messages;
import com.exametrika.common.log.ILogger;
import com.exametrika.common.log.LogLevel;
import com.exametrika.common.log.Loggers;
import com.exametrika.common.services.IService;
import com.exametrika.common.services.IServiceProvider;
import com.exametrika.common.services.IServiceRegistrar;
import com.exametrika.common.services.IServiceRegistry;
import com.exametrika.common.utils.Assert;
import com.exametrika.common.utils.Exceptions;
import com.exametrika.common.utils.Files;
import com.exametrika.common.utils.Objects;


/**
 * The {@link WebServerService} represents a web server service.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public final class WebServerService implements IService, IServiceProvider {
    private static final IMessages messages = Messages.get(IMessages.class);
    private static final ILogger logger = Loggers.get(WebServerService.class);
    private WebServerConfiguration configuration;
    private IServerService serverService;
    private Tomcat server;
    private File workPath;

    @Override
    public void register(IServiceRegistrar registrar) {
        registrar.register("server.web", this);
    }

    @Override
    public void wire(IServiceRegistry registry) {
        serverService = registry.findService(IServerService.NAME);
    }

    @Override
    public synchronized void start(IServiceRegistry registry) {
        Assert.notNull(registry);

        workPath = new File((String) registry.findParameter("work"), "web");
        workPath.mkdirs();
        Files.emptyDir(workPath);

        if (logger.isLogEnabled(LogLevel.DEBUG))
            logger.log(LogLevel.DEBUG, messages.started());
    }

    @Override
    public synchronized void stop(boolean fromShutdownHook) {
        destroyServer();
        configuration = null;

        if (logger.isLogEnabled(LogLevel.DEBUG))
            logger.log(LogLevel.DEBUG, messages.stopped());
    }

    @Override
    public synchronized void setConfiguration(ILoadContext context) {
        WebServerConfiguration configuration = context.get(WebServerConfiguration.SCHEMA);
        Assert.notNull(configuration);

        if (this.configuration == null || !Objects.equals(configuration, this.configuration)) {
            destroyServer();
            this.configuration = configuration;
            createServer();
        } else
            this.configuration = configuration;

        if (logger.isLogEnabled(LogLevel.DEBUG))
            logger.log(LogLevel.DEBUG, messages.configurationUpdated());
    }

    @Override
    public void onTimer(long currentTime) {
    }

    private void createServer() {
        Assert.checkState(server == null);

        Tomcat server = new Tomcat();

        File tmpDir = new File(workPath, "tmp");
        tmpDir.mkdirs();
        server.setBaseDir(tmpDir.getPath());

        server.setHostname(configuration.getName());

        File baseDir = new File(workPath, "webapp");
        baseDir.mkdirs();
        server.getHost().setAppBase(baseDir.getPath());

        server.getEngine().setRealm(new ExaDbRealm(serverService));

        Connector connector = new Connector("org.apache.coyote.http11.Http11NioProtocol");
        connector.setPort(configuration.getPort());
        connector.setAttribute("compression", "on");

        String address = null;
        if (configuration.getBindAddress() != null) {
            try {
                address = InetAddress.getByName(configuration.getBindAddress()).getHostAddress();
            } catch (UnknownHostException e) {
                throw new RuntimeException(e);
            }

            connector.setAttribute("address", address);
        }

        if (configuration.isSecured()) {
            connector.setAttribute("SSLEnabled", true);
            connector.setAttribute("scheme", "https");
            connector.setAttribute("secure", true);
            connector.setAttribute("keystoreFile", configuration.getKeyStorePath());
            connector.setAttribute("keyPass", configuration.getKeyStorePassword());

            if (configuration.getUnsecuredPort() != null) {
                Connector defaultConnector = server.getConnector();
                defaultConnector.setPort(configuration.getUnsecuredPort());
                defaultConnector.setRedirectPort(configuration.getPort());
                if (configuration.getBindAddress() != null)
                    defaultConnector.setAttribute("address", address);
            }
        }

        server.setConnector(connector);
        server.getService().addConnector(connector);

        try {
            Context contextUi = server.addWebapp("", new File(System.getProperty("com.exametrika.home"),
                    "lib/web.ui.war").getPath());
            contextUi.removeLifecycleListener(contextUi.findLifecycleListeners()[0]);
            contextUi.addLifecycleListener(new DefaultWebUiXmlListener());
            contextUi.setParentClassLoader(getClass().getClassLoader());

            Context contextApi = server.addWebapp("api", new File(System.getProperty("com.exametrika.home"),
                    "lib/web.api.war").getPath());
            contextApi.removeLifecycleListener(contextApi.findLifecycleListeners()[0]);
            contextApi.addLifecycleListener(new DefaultWebApiXmlListener());
            contextApi.setParentClassLoader(getClass().getClassLoader());
        } catch (ServletException e) {
            Exceptions.wrapAndThrow(e);
        }

        try {
            server.init();
            server.start();
        } catch (LifecycleException e) {
            throw new RuntimeException(e);
        }

        this.server = server;
    }

    private void destroyServer() {
        if (server != null) {
            try {
                server.stop();
                server.destroy();
            } catch (LifecycleException e) {
                if (logger.isLogEnabled(LogLevel.ERROR))
                    logger.log(LogLevel.ERROR, e);
            }

            server = null;
        }
    }

    private static void initWebUiAppDefaults(Context context) {
        Wrapper servlet = Tomcat.addServlet(context, "default", "org.apache.catalina.servlets.DefaultServlet");
        servlet.setLoadOnStartup(1);
        servlet.setOverridable(true);

        context.addServletMapping("/", "default");
        context.setSessionTimeout(30);

        for (int i = 0; i < DEFAULT_MIME_MAPPINGS.length; )
            context.addMimeMapping(DEFAULT_MIME_MAPPINGS[i++], DEFAULT_MIME_MAPPINGS[i++]);

        context.addWelcomeFile("index.html");
        context.addWelcomeFile("index.htm");
    }

    private static void initWebApiAppDefaults(Context context) {
        Wrapper servlet = Tomcat.addServlet(context, "default", "org.apache.catalina.servlets.DefaultServlet");
        servlet.setLoadOnStartup(1);
        servlet.setOverridable(true);

        context.addServletMapping("/", "default");
        context.setSessionTimeout(30);

        for (int i = 0; i < DEFAULT_MIME_MAPPINGS.length; )
            context.addMimeMapping(DEFAULT_MIME_MAPPINGS[i++], DEFAULT_MIME_MAPPINGS[i++]);
    }

    private class DefaultWebUiXmlListener implements LifecycleListener {
        @Override
        public void lifecycleEvent(LifecycleEvent event) {
            if (Lifecycle.BEFORE_START_EVENT.equals(event.getType()))
                initWebUiAppDefaults((Context) event.getLifecycle());
        }
    }

    private class DefaultWebApiXmlListener implements LifecycleListener {
        @Override
        public void lifecycleEvent(LifecycleEvent event) {
            if (Lifecycle.BEFORE_START_EVENT.equals(event.getType()))
                initWebApiAppDefaults((Context) event.getLifecycle());
        }
    }

    private static final String[] DEFAULT_MIME_MAPPINGS =
            {
                    "abs", "audio/x-mpeg",
                    "ai", "application/postscript",
                    "aif", "audio/x-aiff",
                    "aifc", "audio/x-aiff",
                    "aiff", "audio/x-aiff",
                    "aim", "application/x-aim",
                    "art", "image/x-jg",
                    "asf", "video/x-ms-asf",
                    "asx", "video/x-ms-asf",
                    "au", "audio/basic",
                    "avi", "video/x-msvideo",
                    "avx", "video/x-rad-screenplay",
                    "bcpio", "application/x-bcpio",
                    "bin", "application/octet-stream",
                    "bmp", "image/bmp",
                    "body", "text/html",
                    "cdf", "application/x-cdf",
                    "cer", "application/pkix-cert",
                    "class", "application/java",
                    "cpio", "application/x-cpio",
                    "csh", "application/x-csh",
                    "css", "text/css",
                    "dib", "image/bmp",
                    "doc", "application/msword",
                    "dtd", "application/xml-dtd",
                    "dv", "video/x-dv",
                    "dvi", "application/x-dvi",
                    "eps", "application/postscript",
                    "etx", "text/x-setext",
                    "exe", "application/octet-stream",
                    "gif", "image/gif",
                    "gtar", "application/x-gtar",
                    "gz", "application/x-gzip",
                    "hdf", "application/x-hdf",
                    "hqx", "application/mac-binhex40",
                    "htc", "text/x-component",
                    "htm", "text/html",
                    "html", "text/html",
                    "ief", "image/ief",
                    "jad", "text/vnd.sun.j2me.app-descriptor",
                    "jar", "application/java-archive",
                    "java", "text/x-java-source",
                    "jnlp", "application/x-java-jnlp-file",
                    "jpe", "image/jpeg",
                    "jpeg", "image/jpeg",
                    "jpg", "image/jpeg",
                    "js", "application/javascript",
                    "json", "application/json",
                    "jsf", "text/plain",
                    "jspf", "text/plain",
                    "kar", "audio/midi",
                    "latex", "application/x-latex",
                    "m3u", "audio/x-mpegurl",
                    "mac", "image/x-macpaint",
                    "man", "text/troff",
                    "mathml", "application/mathml+xml",
                    "me", "text/troff",
                    "mid", "audio/midi",
                    "midi", "audio/midi",
                    "mif", "application/x-mif",
                    "mov", "video/quicktime",
                    "movie", "video/x-sgi-movie",
                    "mp1", "audio/mpeg",
                    "mp2", "audio/mpeg",
                    "mp3", "audio/mpeg",
                    "mp4", "video/mp4",
                    "mpa", "audio/mpeg",
                    "mpe", "video/mpeg",
                    "mpeg", "video/mpeg",
                    "mpega", "audio/x-mpeg",
                    "mpg", "video/mpeg",
                    "mpv2", "video/mpeg2",
                    "nc", "application/x-netcdf",
                    "oda", "application/oda",
                    "odb", "application/vnd.oasis.opendocument.database",
                    "odc", "application/vnd.oasis.opendocument.chart",
                    "odf", "application/vnd.oasis.opendocument.formula",
                    "odg", "application/vnd.oasis.opendocument.graphics",
                    "odi", "application/vnd.oasis.opendocument.image",
                    "odm", "application/vnd.oasis.opendocument.text-master",
                    "odp", "application/vnd.oasis.opendocument.presentation",
                    "ods", "application/vnd.oasis.opendocument.spreadsheet",
                    "odt", "application/vnd.oasis.opendocument.text",
                    "otg", "application/vnd.oasis.opendocument.graphics-template",
                    "oth", "application/vnd.oasis.opendocument.text-web",
                    "otp", "application/vnd.oasis.opendocument.presentation-template",
                    "ots", "application/vnd.oasis.opendocument.spreadsheet-template ",
                    "ott", "application/vnd.oasis.opendocument.text-template",
                    "ogx", "application/ogg",
                    "ogv", "video/ogg",
                    "oga", "audio/ogg",
                    "ogg", "audio/ogg",
                    "spx", "audio/ogg",
                    "flac", "audio/flac",
                    "anx", "application/annodex",
                    "axa", "audio/annodex",
                    "axv", "video/annodex",
                    "xspf", "application/xspf+xml",
                    "pbm", "image/x-portable-bitmap",
                    "pct", "image/pict",
                    "pdf", "application/pdf",
                    "pgm", "image/x-portable-graymap",
                    "pic", "image/pict",
                    "pict", "image/pict",
                    "pls", "audio/x-scpls",
                    "png", "image/png",
                    "pnm", "image/x-portable-anymap",
                    "pnt", "image/x-macpaint",
                    "ppm", "image/x-portable-pixmap",
                    "ppt", "application/vnd.ms-powerpoint",
                    "pps", "application/vnd.ms-powerpoint",
                    "ps", "application/postscript",
                    "psd", "image/vnd.adobe.photoshop",
                    "qt", "video/quicktime",
                    "qti", "image/x-quicktime",
                    "qtif", "image/x-quicktime",
                    "ras", "image/x-cmu-raster",
                    "rdf", "application/rdf+xml",
                    "rgb", "image/x-rgb",
                    "rm", "application/vnd.rn-realmedia",
                    "roff", "text/troff",
                    "rtf", "application/rtf",
                    "rtx", "text/richtext",
                    "sh", "application/x-sh",
                    "shar", "application/x-shar",
                    /*"shtml", "text/x-server-parsed-html",*/
                    "sit", "application/x-stuffit",
                    "snd", "audio/basic",
                    "src", "application/x-wais-source",
                    "sv4cpio", "application/x-sv4cpio",
                    "sv4crc", "application/x-sv4crc",
                    "svg", "image/svg+xml",
                    "svgz", "image/svg+xml",
                    "swf", "application/x-shockwave-flash",
                    "t", "text/troff",
                    "tar", "application/x-tar",
                    "tcl", "application/x-tcl",
                    "tex", "application/x-tex",
                    "texi", "application/x-texinfo",
                    "texinfo", "application/x-texinfo",
                    "tif", "image/tiff",
                    "tiff", "image/tiff",
                    "tr", "text/troff",
                    "tsv", "text/tab-separated-values",
                    "txt", "text/plain",
                    "ulw", "audio/basic",
                    "ustar", "application/x-ustar",
                    "vxml", "application/voicexml+xml",
                    "xbm", "image/x-xbitmap",
                    "xht", "application/xhtml+xml",
                    "xhtml", "application/xhtml+xml",
                    "xls", "application/vnd.ms-excel",
                    "xml", "application/xml",
                    "xpm", "image/x-xpixmap",
                    "xsl", "application/xml",
                    "xslt", "application/xslt+xml",
                    "xul", "application/vnd.mozilla.xul+xml",
                    "xwd", "image/x-xwindowdump",
                    "vsd", "application/vnd.visio",
                    "wav", "audio/x-wav",
                    "wbmp", "image/vnd.wap.wbmp",
                    "wml", "text/vnd.wap.wml",
                    "wmlc", "application/vnd.wap.wmlc",
                    "wmls", "text/vnd.wap.wmlsc",
                    "wmlscriptc", "application/vnd.wap.wmlscriptc",
                    "wmv", "video/x-ms-wmv",
                    "wrl", "model/vrml",
                    "wspolicy", "application/wspolicy+xml",
                    "Z", "application/x-compress",
                    "z", "application/x-compress",
                    "zip", "application/zip"
            };

    private interface IMessages {
        @DefaultMessage("Web server service is started.")
        ILocalizedMessage started();

        @DefaultMessage("Web server service is stopped.")
        ILocalizedMessage stopped();

        @DefaultMessage("Configuration of web server service is updated.")
        ILocalizedMessage configurationUpdated();
    }
}
