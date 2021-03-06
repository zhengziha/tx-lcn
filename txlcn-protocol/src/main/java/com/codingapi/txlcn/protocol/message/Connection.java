package com.codingapi.txlcn.protocol.message;

import com.codingapi.txlcn.protocol.await.Lock;
import com.codingapi.txlcn.protocol.await.LockContext;
import com.codingapi.txlcn.protocol.config.Config;
import com.codingapi.txlcn.protocol.exception.ProtocolException;
import com.codingapi.txlcn.protocol.message.separate.SnowFlakeMessage;
import com.codingapi.txlcn.protocol.message.separate.TransactionMessage;
import io.netty.channel.ChannelHandlerContext;
import lombok.Getter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;

/**
 * Maintains a TCP connection between the local peer and a neighbour
 */
public class Connection {

  private static final Logger LOGGER = LoggerFactory.getLogger(Connection.class);

  private final InetSocketAddress remoteAddress;

  private ChannelHandlerContext ctx;

  @Getter
  private final String remoteHost;

  @Getter
  private final int remotePort;

  @Getter
  private final String uniqueKey;

  private final Config config;

  public Connection(ChannelHandlerContext ctx,Config config) {
    this.config = config;
    this.remoteAddress = (InetSocketAddress) ctx.channel().remoteAddress();
    this.ctx = ctx;
    this.remoteHost = remoteAddress.getAddress().getHostAddress();
    this.remotePort = remoteAddress.getPort();
    this.uniqueKey = String.format("%s:%d",remoteHost,remotePort);
  }


  public void send(final Message msg) {
    if (ctx != null) {
      ctx.writeAndFlush(msg);
    } else {
      LOGGER.error("Can not send message " + msg.getClass() + " to " + toString());
    }
  }

  public TransactionMessage request(final TransactionMessage msg) {
    if (ctx != null) {
      String groupId = msg.getGroupId();
      Lock lock = LockContext.getInstance().addKey(groupId);
      try {
        LOGGER.debug("send message {}", msg);
        ctx.writeAndFlush(msg);
        lock.await(config.getAwaitTime());
        return (TransactionMessage) lock.getRes();
      } finally {
        lock.clear();
      }
    } else {
      LOGGER.error("Can not send message " + msg.getClass() + " to " + toString());
      throw new ProtocolException("can't send message . ");
    }
  }

  public SnowFlakeMessage request(final SnowFlakeMessage msg) {
    if (ctx != null) {
      Lock lock = LockContext.getInstance().addKey(msg.getInstanceId());
      try {
        LOGGER.debug("send message {}", msg);
        ctx.writeAndFlush(msg);
        lock.await(config.getAwaitTime());
        return (SnowFlakeMessage) lock.getRes();
      } finally {
        lock.clear();
      }
    } else {
      LOGGER.error("Can not send message " + msg.getClass() + " to " + toString());
      throw new ProtocolException("can't send message . ");
    }
  }

  public void close() {
    LOGGER.debug("Closing session of {}", toString());
    if (ctx != null) {
      ctx.close();
      ctx = null;
    }
  }

  @Override
  public boolean equals(final Object other) {
    if (this == other) {
      return true;
    }
    if (other == null || getClass() != other.getClass()) {
      return false;
    }

    Connection that = (Connection) other;

    return !(uniqueKey != null ? !uniqueKey.equals(that.uniqueKey) : that.uniqueKey != null);
  }

  @Override
  public int hashCode() {
    return uniqueKey != null ? uniqueKey.hashCode() : 0;
  }

  @Override
  public String toString() {
    return "Connection{" +
        "remoteAddress=" + remoteAddress +
        ", isOpen=" + (ctx != null) +
        '}';
  }
}
