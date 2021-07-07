# SE demo

A k8s based deployment designed to demonstrate scalability in a trade generation scenario.

What happens is

1. Trades get generated
2. They get sent to a processing server
3. The server saves the raw trades and aggregate records to an Aerospike database, summarizing trading activity on a per contract basis and also on a per contract & price basis. The summary records are sharded in order to manage load.

## Pre-requisites

You will need _kubectl_ and access to a Kubernetes cluster e.g. Docker Desktop or EKS.

## Quick Start

To run the demo, from the root directory of this repository

```bash
./run-se-demo.sh
```

This will create 3 different microservices

1. A trade generator - this is the trade-data-gen replica set
2. An Aerospike pod
3. A trade processing service called trade-store-server

After creating the above assets, the script will call 

```bash
kubectl get pods
```

every second. You should stop the script once all the pods are in the ```Running``` state. This will take approximately 50 seconds. The output will look something like

```bash
NAME                       READY   STATUS    RESTARTS   AGE
aerospike                  1/1     Running   0          55s
trade-data-gen-v45gt       1/1     Running   3          55s
trade-data-gen-xrc5l       1/1     Running   2          55s
trade-store-server-jfzmg   1/1     Running   2          55s
trade-store-server-rml8n   1/1     Running   2          55s
```

You can see the results of the demo by logging into Aerospike

```
kubectl exec -it aerospike -- aql	
```

We can look at the raw trades

```bash
aql> select * from test.trades
+---------+-------------------+--------+---------------+
| ticker  | price             | volume | timestamp     |
+---------+-------------------+--------+---------------+
| "B.COM" | 123.9984          | 4272   | 1625671381566 |
| "B.COM" | 123.9995          | 21949  | 1625671342500 |
| "A.COM" | 32.0064           | 368    | 1625671378544 |
| "B.COM" | 124.0038          | 16285  | 1625671479635 |
...
```

We can also look at the summary information

```bash
aql> select * from memory_ns.contractSummary
+----------------------------------------------------------------------------+----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------+
| contractRecord                                                             | cntrctPriceSum                                                                                                                                                                                                                                                 |
+----------------------------------------------------------------------------+----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------+
| MAP('{"price":[124.0255], "timestamp":[1625671657808], "volume":1953717}') | KEY_VALUE_ORDERED_MAP('{124.015:{"timestamp":[1625671630351], "volume":1862}, 123.9918:{"timestamp":[1625671533298], "volume":19920}, 123.9933:{"timestamp":[1625671416758], "volume":578}, 123.9974:{"timestamp":[1625671363606], "volume":2303}, 123.9998:{" |
| MAP('{"price":[32.0111], "timestamp":[1625671657809], "volume":463060}')   | KEY_VALUE_ORDERED_MAP('{32.0043:{"timestamp":[1625671460984], "volume":1638}, 31.9968:{"timestamp":[1625671613637], "volume":65}, 31.9913:{"timestamp":[1625671551985], "volume":250}, 31.9851:{"timestamp":[1625671507767], "volume":3527}, 31.9983:{"timesta |
| MAP('{"price":[32.0124], "timestamp":[1625671658449], "volume":327253}')   | KEY_VALUE_ORDERED_MAP('{32.0097:{"timestamp":[1625671647398], "volume":403}, 31.9859:{"timestamp":[1625671649780], "volume":7604}, 32:{"timestamp":[1625671435399], "volume":164}, 31.9953:{"timestamp":[1625671469576], "volume":6637}, 31.9924:{"timestamp": |

```

This contains sharded summary records for scalability purposes

We can retrieve an individual shard using the key ```ticker-shardNo```

```bash
aql> select * from memory_ns.contractSummary where PK = "A.COM-1"
+--------------------------------------------------------------------------+----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------+
| contractRecord                                                           | cntrctPriceSum                                                                                                                                                                                                                                                 |
+--------------------------------------------------------------------------+----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------+
| MAP('{"price":[32.0126], "timestamp":[1625671775814], "volume":477092}') | KEY_VALUE_ORDERED_MAP('{32.0069:{"timestamp":[1625671617274], "volume":3143}, 31.9985:{"timestamp":[1625671603581], "volume":814}, 31.9959:{"timestamp":[1625671319389], "volume":923}, 31.9904:{"timestamp":[1625671548968], "volume":4914}, 31.9892:{"timest |
+--------------------------------------------------------------------------+----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------+
1 row in set (0.002 secs)

OK
```

The level of sharding can be controlled - see later.

## Scaling control

The level of scaling in the deployment can be controlled

1. Increase the number of trade generators by amending the number of replicas specified for the **trade-data-gen** replica set in ```se-demo.yml.template``` - note you must modify the _template_

   ```yml
   apiVersion: apps/v1
   kind: ReplicaSet
   metadata:
     name: trade-data-gen
   spec:
     replicas: 2 # <---- THIS NUMBER
   ```

2. Increase the number of trade processing servers by amending the number of replicas specified for the **trade-store-server** replica set in ```se-demo.yml.template``` 

   ```yml
   apiVersion: apps/v1
   kind: ReplicaSet
   metadata:
     name: trade-store-server
   spec:
     replicas: 2 # <---- THIS NUMBER
   ```

3. The number of trades generated per container per second can be modified via the _ITERATIONS_PER_SECOND_ and _TRADES_PER_ITERATION_ environment variables for the **trade-data-gen** container in ```se-demo.yml.template```

   ```yml
      spec:
         containers:
         - name: trade-data-gen
           image: $DOCKERHUB_ACCOUNT/se-simulator:tradeGen
           env:
           - name: ITERATIONS_PER_SECOND
             value: "1" # <---- THIS NUMBER
           - name: TRADES_PER_ITERATION
             value: "10" # <---- AND THIS ONE
   ```

4. The level of sharding of the aggregate records can be modified via the _CONTRACT_RECORD_SHARD_COUNT_ environment variable for the **trade-store-server** container in ```se-demo.yml.template```

   ```yml
         containers:
         - name: trade-store-server
           image: $DOCKERHUB_ACCOUNT/se-simulator:trade-store-server
           env:
           - name: CONTRACT_RECORD_SHARD_COUNT
             value: "10" # <---- THIS NUMBER
   ```

After changing any of these values the deployment should be deleted and re-deployed.

```bash
./teardown-se-demo.yml
```

followed by

```bsah
./run-se-demo.sh
```

as above

## Teardown

```bash
./teardown-se-demo.yml
```

## Trade Store Server

The Java code associated with this is in the ```TradeStoreServer``` directory. 

The unit tests are a good place to start in terms of understanding how it works.

The following calls

```java
public long getAggregateVolumeForTicker(String ticker)

public double getHighestPriceTradedForTicker(String ticker)

public long getMostRecentTradeTimestampForTicker(String ticker)

public long getAggregateVolumeForTickerAndPrice(String ticker,double price)

public long getMostRecentTradeTimestampForTickerAndPrice(String ticker,double price)
```

in the class ```TradeStoreServer``` can be examined to understand how the net results are obtained from the aggregates

## Building 

You can build your own images by altering the value of the variable **DOCKERHUB_ACCOUNT** in ```respository-env.sh``` to your own DockerHub account id. Remember to do ```docker login``` having done this. Build thusly

```bash
./build-docker-images.sh
```

After this, when you run ```./run-se-demo.sh``` the images from your DockerHub repository will be used.