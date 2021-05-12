package com.example;

import java.util.List;
import java.util.Map;

import javax.annotation.PostConstruct;
import javax.inject.Inject;

import org.cometd.oort.Oort;
import org.cometd.oort.OortList;
import org.cometd.oort.OortObject.Info;
import org.cometd.oort.OortObjectFactories;
import org.eclipse.jetty.util.ajax.JSON;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
public class QueueService {
    private static final Logger LOG = LoggerFactory.getLogger( QueueService.class );

    @Inject
    private Oort oort;
    @Inject
    private MyOortService myOortService;

    private OortList<QueueItem> queue;

    @PostConstruct
    public void init() throws Exception {
        queue = new OortList<>( oort, "my-queue", OortObjectFactories.forConcurrentList() );
        queue.start();
    }

    public void pushToQueue( String id ) {
        queue.addAndShare( null, new QueueItem( id ) );
    }

    @Scheduled( fixedDelay = 10000 )
    private void monitorQueue() {
        pushToQueue( OortConfig.getHost() + " monitored the queue." );

        for( Info<List<QueueItem>> info : queue ) {
            LOG.debug( "Items on {}: {}", info.getOortURL(), info.getObject().size() );
            for( QueueItem item : info.getObject() ) {
                LOG.debug( "Queue item: {}", item );
            }
        }

        // myOortService.broadcast( new QueueItem( OortConfig.getHost() + " monitored the queue." ) );
    }

    public static class QueueItem {
        public final String id;

        public QueueItem( String id ) {
            this.id = id;
        }

        @Override
        public String toString() {
            return "QueueItem [id=" + id + "]";
        }
    }

    public static class QueueItemJsonConvertor implements JSON.Convertor {
        @Override
        public void toJSON( Object obj, JSON.Output out ) {
            QueueItem item = (QueueItem) obj;
            out.addClass( QueueItem.class );
            out.add( "id", item.id );
        }

        @Override
        @SuppressWarnings( "rawtypes" )
        public Object fromJSON( Map obj ) {
            return new QueueItem( (String) obj.get( "id" ) );
        }
    }

}
