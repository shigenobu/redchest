package com.walksocket.rc;

import org.junit.Test;

import java.nio.charset.StandardCharsets;

public class TestRc {

  @Test
  public void test() throws RcServer.RcServerException, RcClient.RcClientException {
    RcLogger.setVerbose(true);
    RcDate.setAddMilliSeconds(32400000);

    RcServer server = new RcServer(new SeverCallback());
    server.start();

    RcClient client = new RcClient(new ClientCallback(), "127.0.0.1", 8710);
    client.connect();

    try {
      Thread.sleep(10000);
    } catch (InterruptedException e) {
      e.printStackTrace();
    }

    client.disconnect();
    server.shutdown();
  }

  public class SeverCallback implements RcCallback {

    @Override
    public void onOpen(RcSession session) {
      RcLogger.debug(() -> String.format("server onOpen:%s", session));

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
      RcLogger.debug(() -> String.format("server onClose:%s %s", session, reason));
    }
  }

  public class ClientCallback implements RcCallback {

    @Override
    public void onOpen(RcSession session) {
      RcLogger.debug(() -> String.format("cleint onOpen:%s", session));

      session.setValue("cnt", 0);
    }

    @Override
    public void onMessage(RcSession session, byte[] message) {
      RcLogger.debug(() -> String.format("client onMessage:%s %s", session, new String(message)));

      int cnt = session.getValue("cnt", Integer.class);
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
      RcLogger.debug(() -> String.format("client onClose:%s %s", session, reason));
    }
  }
}
