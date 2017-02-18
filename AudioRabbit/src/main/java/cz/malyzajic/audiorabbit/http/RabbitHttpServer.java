package cz.malyzajic.audiorabbit.http;

import cz.malyzajic.audiorabbit.ContentNode;
import cz.malyzajic.audiorabbit.IContentContainer;
import fi.iki.elonen.NanoHTTPD;
import fi.iki.elonen.NanoHTTPD.Response.IStatus;
import fi.iki.elonen.NanoHTTPD.Response.Status;
import fi.iki.elonen.SimpleWebServer;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.nio.charset.Charset;
import java.nio.charset.CharsetEncoder;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.logging.Logger;

/**
 *
 * @author daop
 */
public class RabbitHttpServer extends SimpleWebServer {

    private static final String DEFAULT_BINARY_MIME = "application/binary";

    private IContentContainer contentContainer;

    public RabbitHttpServer(String hostname, int port) throws IOException {
        super(hostname, port, Collections.EMPTY_LIST, true);
        start(NanoHTTPD.SOCKET_READ_TIMEOUT, false);
    }

    private static final Logger LOGGER = Logger.getLogger("RabbitHttpServer");

    public void setContentContainer(IContentContainer contentContainer) {
        this.contentContainer = contentContainer;
    }

    @Override
    public Response serve(IHTTPSession session) {

        try {
            Map<String, String> header = session.getHeaders();
            Map<String, String> parms = session.getParms();
            String uri = session.getUri();

            LOGGER.info(session.getMethod() + "" + uri + "");

            Iterator<String> e = header.keySet().iterator();
            while (e.hasNext()) {
                String value = e.next();
            }
            e = parms.keySet().iterator();
            while (e.hasNext()) {
                String value = e.next();
            }

            String itemId = uri.replaceFirst("/", "");
            itemId = URLDecoder.decode(itemId, "UTF-8");
            String newUri = null;

            if (contentContainer.hasNode(itemId)) {
                ContentNode node = contentContainer.getNode(itemId);
                if (node.isItem()) {
                    newUri = node.getFullPath();
                }
            }

            if (newUri != null) {
                uri = newUri;
            }

            String mime = null;
            mime = getMimeByUri(uri);
            return serveFile(uri, header, new File(newUri), mime);
        } catch (UnsupportedEncodingException ex) {

        }
        return null;
    }

    private String getMimeByUri(String uri) {
        String mime = null;
        int dot = uri.lastIndexOf('.');
        if (dot >= 0) {
            mime = (String) theMimeTypes.get(uri.substring(dot + 1).toLowerCase());
        }
        if (mime == null) {
            mime = RabbitHttpServer.DEFAULT_BINARY_MIME;
        }
        return mime;
    }

    /**
     * Serves file from homeDir and its' subdirectories (only). Uses only URI,
     * ignores all headers and HTTP parameters.
     */
    Response serveFile(String uri, Map<String, String> header, File file, String mime) {
        Response res;
        try {
            // Calculate etag
            String etag = Integer.toHexString((file.getAbsolutePath() + file.lastModified() + "" + file.length()).hashCode());

            // Support (simple) skipping:
            long startFrom = 0;
            long endAt = -1;
            String range = header.get("range");
            if (range != null) {
                if (range.startsWith("bytes=")) {
                    range = range.substring("bytes=".length());
                    int minus = range.indexOf('-');
                    try {
                        if (minus > 0) {
                            startFrom = Long.parseLong(range.substring(0, minus));
                            endAt = Long.parseLong(range.substring(minus + 1));
                        }
                    } catch (NumberFormatException ignored) {
                    }
                }
            }

            // get if-range header. If present, it must match etag or else we
            // should ignore the range request
            String ifRange = header.get("if-range");
            boolean headerIfRangeMissingOrMatching = (ifRange == null || etag.equals(ifRange));

            String ifNoneMatch = header.get("if-none-match");
            boolean headerIfNoneMatchPresentAndMatching = ifNoneMatch != null && ("*".equals(ifNoneMatch) || ifNoneMatch.equals(etag));

            // Change return code and add Content-Range header when skipping is
            // requested
            long fileLen = file.length();

            if (headerIfRangeMissingOrMatching && range != null && startFrom >= 0 && startFrom < fileLen) {
                // range request that matches current etag
                // and the startFrom of the range is satisfiable
                if (headerIfNoneMatchPresentAndMatching) {
                    // range request that matches current etag
                    // and the startFrom of the range is satisfiable
                    // would return range from file
                    // respond with not-modified
                    res = newFixedLengthResponse(Status.NOT_MODIFIED, mime, "");
                    res.addHeader("ETag", etag);
                } else {
                    if (endAt < 0) {
                        endAt = fileLen - 1;
                    }
                    long newLen = endAt - startFrom + 1;
                    if (newLen < 0) {
                        newLen = 0;
                    }

                    FileInputStream fis = new FileInputStream(file);
                    fis.skip(startFrom);

                    res = newFixedLengthResponse(Status.PARTIAL_CONTENT, mime, fis, newLen);
                    res.addHeader("Accept-Ranges", "bytes");
                    res.addHeader("Content-Length", "" + newLen);
                    res.addHeader("Content-Range", "bytes " + startFrom + "-" + endAt + "/" + fileLen);
                    res.addHeader("ETag", etag);
                }
            } else {

                if (headerIfRangeMissingOrMatching && range != null && startFrom >= fileLen) {
                    // return the size of the file
                    // 4xx responses are not trumped by if-none-match
                    res = newFixedLengthResponse(Status.RANGE_NOT_SATISFIABLE, NanoHTTPD.MIME_PLAINTEXT, "");
                    res.addHeader("Content-Range", "bytes */" + fileLen);
                    res.addHeader("ETag", etag);
                } else if (range == null && headerIfNoneMatchPresentAndMatching) {
                    // full-file-fetch request
                    // would return entire file
                    // respond with not-modified
                    res = newFixedLengthResponse(Status.NOT_MODIFIED, mime, "");
                    res.addHeader("ETag", etag);
                } else if (!headerIfRangeMissingOrMatching && headerIfNoneMatchPresentAndMatching) {
                    // range request that doesn't match current etag
                    // would return entire (different) file
                    // respond with not-modified

                    res = newFixedLengthResponse(Status.NOT_MODIFIED, mime, "");
                    res.addHeader("ETag", etag);
                } else {
                    // supply the file
                    res = newFixedFileResponse(file, mime);
                    res.addHeader("Content-Length", "" + fileLen);
                    res.addHeader("ETag", etag);
                }
            }
        } catch (IOException ioe) {
            res = getForbiddenResponse("Reading file failed.");
        }

        return res;
    }

    private Response newFixedFileResponse(File file, String mime) throws FileNotFoundException {
        Response res;
        res = newFixedLengthResponse(Status.OK, mime, new FileInputStream(file), (int) file.length());
        res.addHeader("Accept-Ranges", "bytes");
        return res;
    }

    public static Response newFixedLengthResponse(IStatus status, String mimeType, String txt) {
        ContentType contentType = new ContentType(mimeType);
        if (txt == null) {
            return newFixedLengthResponse(status, mimeType, new ByteArrayInputStream(new byte[0]), 0);
        } else {
            byte[] bytes;
            try {
                CharsetEncoder newEncoder = Charset.forName(contentType.getEncoding()).newEncoder();
                if (!newEncoder.canEncode(txt)) {
                    contentType = contentType.tryUTF8();
                }
                bytes = txt.getBytes(contentType.getEncoding());
            } catch (UnsupportedEncodingException e) {
                bytes = new byte[0];
            }
            return newFixedLengthResponse(status, contentType.getContentTypeHeader(), new ByteArrayInputStream(bytes), bytes.length);
        }
    }

    private static HashMap theMimeTypes = new HashMap(30);

    static {
        StringTokenizer st = new StringTokenizer(
                "css		text/css "
                + "js			text/javascript "
                + "htm		text/html "
                + "html		text/html "
                + "txt		text/plain "
                + "asc		text/plain "
                + "gif		image/gif "
                + "jpg		image/jpeg "
                + "jpeg		image/jpeg "
                + "png		image/png "
                + "mp3		audio/mpeg "
                + "m3u		audio/mpeg-url "
                + "pdf		application/pdf "
                + "doc		application/msword "
                + "ogg		application/x-ogg "
                + "zip		application/octet-stream "
                + "exe		application/octet-stream "
                + "class		application/octet-stream ");
        while (st.hasMoreTokens()) {
            theMimeTypes.put(st.nextToken(), st.nextToken());
        }
    }

}
