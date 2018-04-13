# redchest - Java NIO2 Tcp server & client 

[![Build Status](https://travis-ci.org/shigenobu/redchest.svg?branch=master)](https://travis-ci.org/shigenobu/redchest)
[![Coverage Status](https://coveralls.io/repos/github/shigenobu/redchest/badge.svg?branch=master)](https://coveralls.io/github/shigenobu/redchest?branch=master)


## how to use

### for server

    RcServer server = new RcServer(new RcCallback() {
      @Override
      public void onOpen(RcSession session) {
        // --------------------
        // when accepted, once called

        // init message counter
        session.setValue("cnt", 0);

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

        // increment message counter
        int cnt = session.getValue("cnt", Integer.class);
        session.setValue("cnt", cnt++);

        // send message or close session
        if (cnt < 3) {
          String reply = "hi! I am server !";
          try {
            session.send(reply.getBytes());
          } catch (RcSession.RcSendException e) {
            e.printStackTrace();
          }
        } else {
          session.close();
        }
      }

      @Override
      public void onClose(RcSession session, RcCloseReason reason) {
        // --------------------
        // when closed, once called
        System.out.println(String.format("close, reason:%s", reason));
      }
    });
    server.start();
    // wait for ...
    server.shutdown();

### for client

    RcClient client = new RcClient(new RcCallback() {
      @Override
      public void onOpen(RcSession session) {
        // --------------------
        // when connected, once called

        // init message counter
        session.setValue("cnt", 0);

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

        // increment message counter
        int cnt = session.getValue("cnt", Integer.class);
        session.setValue("cnt", cnt++);

        // send message or close session
        if (cnt < 3) {
          String reply = "hi! I am client !";
          try {
            session.send(reply.getBytes());
          } catch (RcSession.RcSendException e) {
            e.printStackTrace();
          }
        } else {
          session.close();
        }
      }

      @Override
      public void onClose(RcSession session, RcCloseReason reason) {
        // --------------------
        // when closed, once called
        System.out.println(String.format("close, reason:%s", reason));
      }
    }, "127.0.0.1", 8710);
    client.connect();
    // wait for ...
    client.disconnect();
