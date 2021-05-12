package com.example;

import javax.annotation.PostConstruct;
import javax.servlet.ServletContext;

import org.cometd.annotation.server.ServerAnnotationProcessor;
import org.cometd.bayeux.server.BayeuxServer;
import org.cometd.bayeux.server.SecurityPolicy;
import org.cometd.bayeux.server.ServerMessage;
import org.cometd.bayeux.server.ServerSession;
import org.cometd.common.JettyJSONContextClient;
import org.cometd.oort.Oort;
import org.cometd.oort.Seti;
import org.cometd.server.AbstractServerTransport;
import org.cometd.server.BayeuxServerImpl;
import org.cometd.server.DefaultSecurityPolicy;
import org.cometd.server.JettyJSONContextServer;
import org.cometd.server.ext.AcknowledgedMessagesExtension;
import org.cometd.server.websocket.javax.WebSocketTransport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.DestructionAwareBeanPostProcessor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.context.ServletContextAware;

@Configuration
public class BayeuxConfigurer implements DestructionAwareBeanPostProcessor, ServletContextAware {
    private static final Logger LOG = LoggerFactory.getLogger( BayeuxConfigurer.class );

    private ServletContext servletContext;
    private ServerAnnotationProcessor processor;

    @PostConstruct
    private void init() {
        BayeuxServer bayeuxServer = bayeuxServer();
        bayeuxServer.setSecurityPolicy( policy() );
        this.processor = new ServerAnnotationProcessor( bayeuxServer );
    }

    @Override
    public Object postProcessBeforeInitialization( Object bean, String name ) throws BeansException {
        processor.processDependencies( bean );
        processor.processConfigurations( bean );
        processor.processCallbacks( bean );
        return bean;
    }

    @Override
    public Object postProcessAfterInitialization( Object bean, String name ) throws BeansException {
        return bean;
    }

    @Override
    public void postProcessBeforeDestruction( Object bean, String name ) throws BeansException {
        processor.deprocessCallbacks( bean );
    }

    @Override
    public void setServletContext( ServletContext servletContext ) {
        this.servletContext = servletContext;
    }

    @Bean( initMethod = "start", destroyMethod = "stop" )
    public BayeuxServer bayeuxServer() {
        BayeuxServerImpl bayeuxServer = new BayeuxServerImpl();
        bayeuxServer.setOption( ServletContext.class.getName(), servletContext );
        bayeuxServer.setOption( AbstractServerTransport.META_CONNECT_DELIVERY_OPTION, true );
        bayeuxServer.setOption( AbstractServerTransport.MAX_QUEUE_OPTION, 10000 );
        bayeuxServer.setOption( AbstractServerTransport.TIMEOUT_OPTION, 60000 );
        bayeuxServer.setOption( AbstractServerTransport.MAX_INTERVAL_OPTION, 1000 );
        bayeuxServer.setOption( WebSocketTransport.PREFIX + "." + WebSocketTransport.COMETD_URL_MAPPING_OPTION, "/cometd/*" );
        bayeuxServer.setOption( WebSocketTransport.PREFIX + "." + WebSocketTransport.MESSAGES_PER_FRAME_OPTION, 2 );
        bayeuxServer.setOption( WebSocketTransport.PREFIX + "." + WebSocketTransport.MAX_MESSAGE_SIZE_OPTION,
                10 * 1024 * 1024 );
        bayeuxServer.setOption( AbstractServerTransport.JSON_CONTEXT_OPTION, new CometdJsonContextServer() );
        bayeuxServer.setOption( BayeuxServerImpl.BROADCAST_TO_PUBLISHER_OPTION, false );
        bayeuxServer.addExtension( new AcknowledgedMessagesExtension() );
        servletContext.setAttribute( BayeuxServer.ATTRIBUTE, bayeuxServer );
        return bayeuxServer;
    }

    @Bean( name = "oort", initMethod = "start", destroyMethod = "stop" )
    public Oort oort() {
        String host = OortConfig.getHost();
        final Oort oort = new Oort( bayeuxServer(), OortConfig.getOortUrl( host ) );
        oort.setAckExtensionEnabled( true );
        oort.setJSONContextClient( new CometdJsonContextClient() );
        oort.setSecret( "hello" );

        servletContext.setAttribute( Oort.OORT_ATTRIBUTE, oort );

        return oort;
    }

    @Bean( name = "seti", initMethod = "start", destroyMethod = "stop" )
    public Seti seti() {
        final Seti seti = new Seti( oort() );
        servletContext.setAttribute( Seti.SETI_ATTRIBUTE, seti );
        return seti;
    }

    @Bean
    public SecurityPolicy policy() {
        return new OortSecurityPolicy( oort() );
    }

    static class CometdJsonContextServer extends JettyJSONContextServer {
        public CometdJsonContextServer() {
            getJSON().addConvertor( QueueService.QueueItem.class, new QueueService.QueueItemJsonConvertor() );
        }
    }

    static class CometdJsonContextClient extends JettyJSONContextClient {
        public CometdJsonContextClient() {
            getJSON().addConvertor( QueueService.QueueItem.class, new QueueService.QueueItemJsonConvertor() );
        }
    }

    static class OortSecurityPolicy extends DefaultSecurityPolicy {

        private final Oort oort;

        public OortSecurityPolicy( Oort oort ) {
            this.oort = oort;
        }

        @Override
        public boolean canHandshake( BayeuxServer server, ServerSession session, ServerMessage message ) {
            if( session.isLocalSession() ) {
                return true;
            }

            if( oort.isOortHandshake( message ) ) {
                return true;
            }

            if( !super.canHandshake( server, session, message ) ) {
                return false;
            }

            return true;
        }
    }
}
