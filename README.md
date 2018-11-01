# redchest - Java NIO2 Tcp server & client 

[![Maven Central](https://maven-badges.herokuapp.com/maven-central/com.walksocket/redchest/badge.svg)](https://maven-badges.herokuapp.com/maven-central/com.walksocket/redchest)
[![Javadoc](https://javadoc-badge.appspot.com/com.walksocket/redchest.svg?label=javadoc)](https://javadoc-badge.appspot.com/com.walksocket/redchest)
[![Build Status](https://travis-ci.org/shigenobu/redchest.svg?branch=master)](https://travis-ci.org/shigenobu/redchest)
[![Coverage Status](https://coveralls.io/repos/github/shigenobu/redchest/badge.svg?branch=master)](https://coveralls.io/github/shigenobu/redchest?branch=master)
[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://opensource.org/licenses/Apache-2.0)

## maven

    <dependency>
      <groupId>com.walksocket</groupId>
      <artifactId>redchest</artifactId>
      <version>0.0.6</version>
    </dependency>

## how to use

### for server

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
    // wait for ...
    server.shutdown();

### for client

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

        // send message or close session
        if (cnt < 5) {
          String reply = "hi! I am client ! cnt is " + cnt;
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
        System.out.println(String.format("client close, reason:%s", reason));
      }
    }, "127.0.0.1", 8710);
    client.connect();
    // wait for ...
    client.disconnect();
