package com.example;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.PostConstruct;
import javax.inject.Inject;

import org.cometd.client.BayeuxClient;
import org.cometd.oort.Oort;
import org.cometd.oort.OortComet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class OortConfig {
    private static final Logger LOG = LoggerFactory.getLogger( OortConfig.class );

    @Inject
    private Oort oort;
    @Inject
    private QueueService queue;

    @PostConstruct
    public void init() {
        String host = getHost();
        LOG.debug( "I am {}", host );

        List<String> nodes = new ArrayList<>();
        nodes.add( "localhost:8080" );
        nodes.add( "localhost:8081" );

        for( final String node : nodes ) {
            if( node.equals( host ) ) {
                continue;
            }

            final String url = getOortUrl( node );
            LOG.debug( "Trying to connect to Oort comet {}", url );
            final OortComet oortComet = oort.observeComet( url );
            if( !oortComet.waitFor( 3000, BayeuxClient.State.CONNECTED ) ) {
                LOG.error( "Cannot connect to Oort comet {}", url );
            } else {
                queue.pushToQueue( "test1" );
            }
        }
    }

    public static String getOortUrl( String node ) {
        return "http://" + node + "/cometd";
    }

    public static String getHost() {
        return "localhost:" + System.getenv( "PORT" );
    }

}
