# FoodTruckFinder

A command line utility that lists out available food trucks for SF residents.

# Requirements

* Java8
* Maven

# How to build

Clone and `cd` into the repository.

```bash
$ mvn clean install
$ mvn exec:java
```

The tool is also packaged as a shaded jar:

```
$ java -jar target/food-truck-finder-0.0.1-SNAPSHOT.jar
```

# Implementation notes

Given the context, I chose brevity above all else. Although tests are not 
provided, the core functionality can be tested by unit-testing the following
two methods:

```java
public static JSONArray getTrucks(String resource, String day, String time24) throws UnirestException

public static void renderToConsole(JSONArray trucks, Console console, int pageSize, int colwidth)
```

# How do we turn this into proper web service?

Our webservice may need to provide a paginated API with parameterized date to 
facilitate consistent paging.

```
GET /trucks?date=<ISO 8601 date>&offset=x&limit=y
```

Our nicely formatted output could be returned as content-type text/plain. A 
minor detail is to determine how should the table header be handled.

To support this interface, our implementation needs the ability to render to an
`outputStream` a section of the JSONArray of trucks that starts at `offset` and
is of `limit` length at most. An offset out of bounds would return a 404.

```java

public static void renderToStream(JSONArray trucks, OutputStream os, int offset, int limit) {
  if (offset >= trucks.length) {
    return;
  }
  for (int i = offset; i < Math.min(offset+limit, trucks.length); ++i) {
     JSONObject truck = (JSONObject)trucks.get(i);
     // render truck to the outputStream
  } 
}

```

To complete the implementation, an HttpServlet can extract the query parameters,
call to the origin to retrieve the current set of trucks and render the requested section.


## Scaling

Socrata allows 1000req/hour with a developer key. To function properly, our 
webservice needs to operate within that limit. Working to our advantage is the
proper set of cache control headers provided by the Socrata API. This enables
our solution to use standard proxy configurations as part of our solution.

Specifically here, I would recommend a setup allowing the proxy to return
stale data in order to prevent simultaneous requests to our webservice to spike
at the origin and exhaust the requests per hour allocations. This feature is
supported by both NGinx and Varnish.

If a proxy is not acceptable, an in memory LRU may be suitable (eg: guava cache), 
but special care needs to be taken on how we decide to handle concurrent requests 
to the origin.  One option is to risk getting throttled by the origin by allowing 
all cache misses to concurrently retrieve form the origin, or to synchronize 
the misses behind a single retriever thread. 

A final option would be to eschew filtering and ordering at the origin and 
instead move those operations within the webservice. Our webservice could
periodically poll the origin for an update to the data set and atomically swap
it in the webservice when it has changed. By leveraging the ETAG, this is a very
efficient operation, and unlike all previous in-memory LRU approach, the 
performance of every GET operation to our service is independent of the 
performance at the origin (The proxy solution can also provide this guarantee).



