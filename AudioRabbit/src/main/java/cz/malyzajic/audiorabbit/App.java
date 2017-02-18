package cz.malyzajic.audiorabbit;

import cz.malyzajic.audiorabbit.http.RabbitHttpServer;
import java.io.IOException;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Enumeration;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.fourthline.cling.UpnpService;
import org.fourthline.cling.UpnpServiceImpl;
import org.fourthline.cling.binding.LocalServiceBindingException;
import org.fourthline.cling.binding.annotations.AnnotationLocalServiceBinder;
import org.fourthline.cling.model.DefaultServiceManager;
import org.fourthline.cling.model.ValidationException;
import org.fourthline.cling.model.meta.DeviceDetails;
import org.fourthline.cling.model.meta.DeviceIdentity;
import org.fourthline.cling.model.meta.Icon;
import org.fourthline.cling.model.meta.LocalDevice;
import org.fourthline.cling.model.meta.LocalService;
import org.fourthline.cling.model.meta.ManufacturerDetails;
import org.fourthline.cling.model.meta.ModelDetails;
import org.fourthline.cling.model.types.DeviceType;
import org.fourthline.cling.model.types.UDADeviceType;
import org.fourthline.cling.model.types.UDN;
import org.fourthline.cling.registry.RegistrationException;
import org.fourthline.cling.support.connectionmanager.ConnectionManagerService;
import org.fourthline.cling.support.model.Protocol;
import org.fourthline.cling.support.model.ProtocolInfo;
import org.fourthline.cling.support.model.ProtocolInfos;
import org.fourthline.cling.support.xmicrosoft.AbstractMediaReceiverRegistrarService;

/**
 * Hello world!
 *
 */
public class App implements Runnable {

    public static final String fullAppName = "Audio Rabbit Server";
    public static final String shorAppName = "AR Server";
    public static final String shortVersionString = "0.1";
    public static final String fullVersionString = "0.1 version";
    public static final String fullManufacturer = "David Opletal";
    public static final String shortManufacturer = "dave@page";

    private final static int PORT = 59160;
    private static InetAddress localAddress;

    private RabbitHttpServer httpServer;
    private final RabitConfiguration configuration = new RabitConfiguration();

    private IContentContainer container;

    public static void main(String[] args) throws Exception {
        // Start a user thread that runs the UPnP stack
        Thread serverThread = new Thread(new App());
        serverThread.setDaemon(false);
        serverThread.start();
    }

    public void run() {
        try {
            localAddress = InetAddress.getLocalHost();
            MediaFinder finder = new MediaFinder();
            finder.setConfiguration(configuration);
            container = new ContentTree();
            container.setFiller(finder);
            container.fillContainer();
            httpServer = new RabbitHttpServer(getLocalAddress(), PORT);
            httpServer.setContentContainer(container);
            final UpnpService upnpService = new UpnpServiceImpl();

            Runtime.getRuntime().addShutdownHook(new Thread() {
                @Override
                public void run() {
                    upnpService.shutdown();
                }
            });

            // Add the bound local device to the registry
            upnpService.getRegistry().addDevice(createDevice());

        } catch (IOException | LocalServiceBindingException | ValidationException | RegistrationException ex) {
            System.err.println("Exception occured: " + ex);
            ex.printStackTrace(System.err);
            System.exit(1);
        }
    }

    LocalDevice createDevice()
            throws ValidationException, LocalServiceBindingException, IOException {

        DeviceIdentity identity
                = new DeviceIdentity(
                        UDN.uniqueSystemIdentifier(App.fullAppName + App.fullVersionString)
                );

        DeviceType type = new UDADeviceType("MediaServer", 1);

        URL baseUrl = new URL("http://" + localAddress.getHostAddress());
        URI presentationUri = null;
        try {
            presentationUri = new URI("");
        } catch (URISyntaxException ex) {
            Logger.getLogger(App.class.getName()).log(Level.SEVERE, null, ex);
        }
        DeviceDetails details = new DeviceDetails(baseUrl,
                App.fullAppName,
                new ManufacturerDetails(App.fullManufacturer),
                new ModelDetails(
                        App.fullAppName,
                        App.shorAppName,
                        App.fullVersionString
                ),
                "ser-0-1",
                "",
                presentationUri);

        DeviceDetails details1 = new DeviceDetails(App.fullAppName, new ManufacturerDetails(App.fullManufacturer),
                new ModelDetails(
                        App.fullAppName,
                        App.shorAppName,
                        App.fullVersionString
                ));

        Icon icon = null;

        LocalService<MP3ContentDirectory> service
                = new AnnotationLocalServiceBinder().read(MP3ContentDirectory.class);

        service.setManager(
                new DefaultServiceManager(service, MP3ContentDirectory.class)
        );
        service.getManager().getImplementation().setConteiner(container);
        LocalService<ConnectionManagerService> service1
                = new AnnotationLocalServiceBinder().read(ConnectionManagerService.class);

        final ProtocolInfos sourceProtocols
                = new ProtocolInfos(
                        new ProtocolInfo(
                                Protocol.HTTP_GET,
                                ProtocolInfo.WILDCARD,
                                "audio/mpeg",
                                "DLNA.ORG_PN=MP3;DLNA.ORG_OP=01"
                        ),
                        new ProtocolInfo(
                                Protocol.HTTP_GET,
                                ProtocolInfo.WILDCARD,
                                "video/mpeg",
                                "DLNA.ORG_PN=MPEG1;DLNA.ORG_OP=01;DLNA.ORG_CI=0"
                        )
                );
        service1.setManager(new DefaultServiceManager<ConnectionManagerService>(service1, null) {
            @Override
            protected ConnectionManagerService createServiceInstance() throws Exception {
                return new ConnectionManagerService(sourceProtocols, null);
            }
        });

        LocalService<AbstractMediaReceiverRegistrarService> service2
                = new AnnotationLocalServiceBinder().read(AbstractMediaReceiverRegistrarService.class);

        return new LocalDevice(identity, type, details, icon, new LocalService[]{service, service1, service2});

    }

    public static String getAddress() {
        return getLocalAddress() + ":" + PORT;
    }

    public static String getLocalAddress() {
        String result = null;
        Enumeration<NetworkInterface> interfaces = null;
        try {
            interfaces = NetworkInterface.getNetworkInterfaces();
        } catch (SocketException e) {
            // handle error
        }

        if (interfaces != null) {
            while (interfaces.hasMoreElements() && result == null) {
                NetworkInterface i = interfaces.nextElement();
                Enumeration<InetAddress> addresses = i.getInetAddresses();
                while (addresses.hasMoreElements() && (result == null || result.isEmpty())) {
                    InetAddress address = addresses.nextElement();
                    if (!address.isLoopbackAddress()
                            && address.isSiteLocalAddress() && address.getHostAddress().contains("192.168.")) {
                        result = address.getHostAddress();
                    }
                }
            }
        }
        return result;
    }

}
