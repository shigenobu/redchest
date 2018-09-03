package com.walksocket.rc;

import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Executors;

public class TestRc2 {

  @Test
  public void test() throws RcServer.RcServerException, RcClient.RcClientException {
    RcLogger.setVerbose(true);
    RcDate.setAddMilliSeconds(32400000);

    RcServer server = new RcServer(new SeverCallback());
    server.backlog(128);
    server.bind("0.0.0.0", 8710);
    server.devide(5);
    server.readBufferSize(256);
    server.pool(Executors.newFixedThreadPool(2));
    server.receiveBufferSize(1024 * 1024 * 8);
    server.shutdownExecutor(new ServerCutomExecutor());
    server.start();

    int size = 5;
    List<RcClient> clients = new ArrayList<>();
    for (int i = 0; i < size; i++) {
      RcClient client = new RcClient(new ClientCallback(), "127.0.0.1", 8710);
      client.readBufferSize(256);
      client.shutdownExecutor(new ClientCutomExecutor(i));

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

  public class ServerCutomExecutor implements RcShutdownExecutor {

    @Override
    public void execute() {
      RcLogger.debug("server shutdown execute.");
    }
  }

  public class ClientCutomExecutor implements RcShutdownExecutor {

    private int i;

    public ClientCutomExecutor(int i) {
      this.i = i;
    }

    @Override
    public void execute() {
      RcLogger.debug("client shutdown execute. no:" + i);
    }
  }
}
