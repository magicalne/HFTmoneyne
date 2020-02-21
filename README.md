# HFTmoneyne

Just opensource it if anyone need it.

This project was used to "market making" on bitmex. Tried with some fancy work but failed still.

## Why failed?

1. I use Netty. Netty is good for high throughput scenario. As Netty creates a lot garbage objects. So "stop-the-world" happens.
2. Need better JSON Serde.
3. Didn't figure out a strategy to handle Bitmex lag.

## Still worth a shot

I had a lot of fun in this project although it is failed.

### 1. Write a http client with Netty.

Actually this is not a standard http client. 

For a single request, most http clients will open up a connection, and send some stuffs, then close this connection.
As http1.0 specification goes, http1.0 is syncronize. Which means one request pairs with one response.
Open a new connection is way too heavy for low latency programs. So I maintain one tcp connection for all the http request.
Normally bitmex will shut me down if there is no request for 1 min. So I use "health check" api as a heartbeat.

Interesting, right?

The consequence is clear. The http is not sync any more. I just use http protocol to parse content on tcp only.
So event-diven archetecture is needed. 

### 2. Disruptor

I use disruptor to consume market data on the same core. Say goodbye to cache-miss. 

## Suggestions

For anyone would like to build a HFT system with java. You'd better off switch to another language. 
Or you can write a lot of libs by yourself. Network IO, non-blocking, user space bypass kernel, fast json parser.
The code optimization make it taste not like Java at all.
