/* (C) Brady Russell 2021 */
package com.bradyrussell.uiscoin.node;

import java.net.InetAddress;
import java.util.Objects;

public class PeerAddress {
    private final InetAddress address;
    private final int port;

    public PeerAddress(InetAddress address, int port) {
        this.address = address;
        this.port = port;
    }

    public InetAddress getAddress() {
        return address;
    }

    public int getPort() {
        return port;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PeerAddress that = (PeerAddress) o;
        return getPort() == that.getPort() && Objects.equals(getAddress(), that.getAddress());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getAddress(), getPort());
    }

    @Override
    public String toString() {
        return address.getHostAddress() + ":" + port;
    }
}
