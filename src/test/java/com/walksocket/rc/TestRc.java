package com.walksocket.rc;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class TestRc {

  @Test
  public void test() throws RcServer.RcServerException, RcClient.RcClientException {
    RcLogger.setVerbose(true);
    RcDate.setAddMilliSeconds(32400000);

    RcServer server = new RcServer(new SeverCallback());
    server.backlog(128);
    server.bind("0.0.0.0", 18710);
    server.devide(5);
    server.readBufferSize(256);
    server.pool(Executors.newFixedThreadPool(2));
    server.receiveBufferSize(1024 * 1024 * 8);
    server.start();

    int size = 5;
    List<RcClient> clients = new ArrayList<>();
    for (int i = 0; i < size; i++) {
      RcClient client = new RcClient(new ClientCallback(), "127.0.0.1", 18710);
      client.readBufferSize(256);

      clients.add(client);
      clients.get(i).connect();
    }

    try {
      for (int i = 0; i < 10; i++) {
        RcLogger.debug(String.format("count:%s", server.getSessionCount()));
        Thread.sleep(300);
      }
    } catch (InterruptedException e) {
      e.printStackTrace();
    }

    for (int i = 0; i < clients.size(); i++) {
      clients.get(i).disconnect();
    }
    server.shutdown();
  }

  public class SeverCallback implements RcCallback {

    @Override
    public void onOpen(RcSession session) {
      RcLogger.debug(() -> String.format("server onOpen:%s %s", session.getRemoteAddress(), session));
      session.setIdleMilliSeconds(10000);

      byte[] message = "hello".getBytes(StandardCharsets.UTF_8);
      try {
        session.send(message);
      } catch (RcSession.RcSendException e) {
        e.printStackTrace();
      }
    }

    @Override
    public void onMessage(RcSession session, byte[] message) {
      RcLogger.debug(() -> String.format("server onMessage:%s %s", session, new String(message)));

      byte[] msg = "hi client".getBytes(StandardCharsets.UTF_8);
      try {
        session.send(msg);
      } catch (RcSession.RcSendException e) {
        e.printStackTrace();
      }
    }

    @Override
    public void onClose(RcSession session, RcCloseReason reason) {
      RcLogger.debug(() -> String.format("server onClose:%s %s %s", session.getRemoteAddress(), session, reason));
    }
  }

  public class ClientCallback implements RcCallback {

    @Override
    public void onOpen(RcSession session) {
      RcLogger.debug(() -> String.format("cleint onOpen:%s %s", session.getLocalAddress(), session));

      session.setValue("cnt", 0);
    }

    @Override
    public void onMessage(RcSession session, byte[] message) {
      RcLogger.debug(() -> String.format("client onMessage:%s %s", session, new String(message)));

      int cnt = 0;
      Optional<Integer> opt = session.getValue("cnt", Integer.class);
      if (opt.isPresent()) {
        cnt = opt.get();
      }
      if (cnt < 3) {
        byte[] msg = ("hi server " + cnt).getBytes(StandardCharsets.UTF_8);
        try {
          session.send(msg);
        } catch (RcSession.RcSendException e) {
          e.printStackTrace();
        }
        session.setValue("cnt", cnt + 1);
      } else {
        session.close();
      }
    }

    @Override
    public void onClose(RcSession session, RcCloseReason reason) {
      RcLogger.debug(() -> String.format("client onClose:%s %s %s", session.getLocalAddress(), session, reason));

      session.clearValue("cnt");
      Optional<Integer> opt = session.getValue("cnt", Integer.class);
      if (opt.isPresent()) {
        RcLogger.error("cnt:" + opt.get());
      }
    }
  }

  @Test
  public void testSample() throws RcServer.RcServerException, RcClient.RcClientException, InterruptedException {
    RcLogger.setVerbose(true);
    RcServer server = new RcServer(new RcCallback() {
      @Override
      public void onOpen(RcSession session) {
        // --------------------
        // when accepted, once called

        // send message
        String reply = "hello, client!";
        try {
          session.send(reply.getBytes());
        } catch (RcSession.RcSendException e) {
          e.printStackTrace();
        }
      }

      @Override
      public void onMessage(RcSession session, byte[] message) {
        // --------------------
        // when messaged, any called
        System.out.println(String.format("server onMessage:%s (%s)", new String(message), session));

        // increment message counter
        int cnt = 0;
        Optional<Integer> opt = session.getValue("cnt", Integer.class);
        if (opt.isPresent()) {
          cnt = opt.get();
        }
        session.setValue("cnt", ++cnt);
        System.out.println("server cnt:" + cnt);

        // send message or close session
        if (cnt < 5) {
          String reply = "hi! I am server ! cnt is " + cnt;
          try {
            session.send(reply.getBytes());
          } catch (RcSession.RcSendException e) {
            e.printStackTrace();
          }
        } else {
          session.close();

          String reply = "hi! I am server ! cnt is " + ++cnt;
          try {
            session.send(reply.getBytes());
          } catch (RcSession.RcSendException e) {
            e.printStackTrace();
          }
        }
      }

      @Override
      public void onClose(RcSession session, RcCloseReason reason) {
        // --------------------
        // when closed, once called
        System.out.println(String.format("server close, reason:%s", reason));
      }
    });
    server.start();

    RcClient client = new RcClient(new RcCallback() {
      @Override
      public void onOpen(RcSession session) {
        // --------------------
        // when connected, once called

        // send message
        String reply = "hello, server!";
        try {
          session.send(reply.getBytes());
        } catch (RcSession.RcSendException e) {
          e.printStackTrace();
        }
      }

      @Override
      public void onMessage(RcSession session, byte[] message) {
        // --------------------
        // when messaged, any called
        System.out.println(String.format("client onMessage:%s (%s)", new String(message), session));

        // increment message counter
        int cnt = 0;
        Optional<Integer> opt = session.getValue("cnt", Integer.class);
        if (opt.isPresent()) {
          cnt = opt.get();
        }
        session.setValue("cnt", ++cnt);
        System.out.println("client cnt:" + cnt);

        // send message or close session
        if (cnt < 5) {
          String reply = "hi! I am client ! cnt is " + cnt;
          try {
            session.send(reply.getBytes());
          } catch (RcSession.RcSendException e) {
            e.printStackTrace();
          }
        } else {
//          session.close();
        }
      }

      @Override
      public void onClose(RcSession session, RcCloseReason reason) {
        // --------------------
        // when closed, once called
        System.out.println(String.format("client close, reason:%s", reason));
      }
    }, "127.0.0.1", 8710);
    client.connect();

    Thread.sleep(5000);

    client.disconnect();
    server.shutdown();
  }
}
