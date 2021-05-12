package com.example;

import java.util.HashMap;
import java.util.Map;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import com.example.QueueService.QueueItem;

import org.cometd.oort.Oort;
import org.cometd.oort.OortService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class MyOortService extends OortService<Map<String, Object>, MyOortService.Context<Map<String, Object>>> {
    private static final Logger LOG = LoggerFactory.getLogger( MyOortService.class );

    @Autowired
    public MyOortService( final Oort oort ) {
        super( oort, "my-oort-service" );
        setTimeout( 2 * 1000 );
    }

    @PostConstruct
    private void init() throws Exception {
        start();
    }

    @PreDestroy
    private void shutdown() throws Exception {
        stop();
    }

    @Override
    protected void onForwardSucceeded( Map<String, Object> result, Context<Map<String, Object>> context ) {
        context.callback.succeeded( result );
    }

    @Override
    protected void onForwardFailed( Object failure, Context<Map<String, Object>> context ) {
        context.callback.failed( failure );
    }

    @Override
    protected Result<Map<String, Object>> onForward( Request request ) {
        final Map<String, Object> data = request.getDataAsMap();
        try {
            final QueueItem item = (QueueItem) data.get( "item" );
            LOG.info( "Received item {}", item );
            return Result.success( null );
        } catch( Exception e ) {
            LOG.warn( "Cannot process a message {}: {}", data, e.getMessage() );
            return Result.failure( "Cannot process message" );
        }
    }

    public void broadcast( final QueueItem msg ) {
        final Map<String, Object> data = new HashMap<>();
        data.put( "item", msg );
        forward( null, data, new Context<>( new Callback<Map<String, Object>>() {
            @Override
            public void succeeded( final Map<String, Object> result ) {
                LOG.info( "Sent data successfully." );
            }

            @Override
            public void failed( final Object failure ) {
                LOG.info( "Couldn't send data." );
            }
        } ) );
    }

    protected interface Callback<T> {
        void succeeded( final T result );

        void failed( final Object failure );
    }

    protected static class Context<T> {
        final Callback<T> callback;

        public Context( final Callback<T> callback ) {
            this.callback = callback;
        }
    }
}
