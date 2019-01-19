# Sentinel 实战：使用控制台管理规则

通过 sentinel 的控制台，我们可以对规则进行查询和修改，也可以查看到实时监控，机器列表等信息，所以我们需要对 sentinel 的控制台做个完整的了解。


## 部署控制台

首先需要启动控制台，  sentinel 的控制台是用 spring boot 写的一个web 应用，我们有几种方式来获取控制台：

### 下载可执行 jar 包

从 [release 页面](https://github.com/alibaba/Sentinel/releases) 下载截止目前为止最新版本的控制台 jar 包，如下图所示：

![sentinel-dashboard-release-jar-1](images/sentinel-dashboard-release-jar-1.png)

### 下载源码构建

除了可以下载预先构建好的可执行 jar 包之外，我们还可以把控制台的工程下载下来自行用源码构建，sentinel 是一个多 maven 模块的项目，控制台是其中的一个项目，如下图所示：

![sentinel-dashboard-module-1](images/sentinel-dashboard-module-1.png)

如上图所示，我们可以下载完整的 sentinel 的项目，然后构建其中的 sentinel-dashboard 模块，也可以只下载 sentinel-dashboard 模块然后构建。

这里我选择将完整的 sentinel 工程下载下来，然后构建 sentinel-dashboard 模块，首先在项目根目录下执行：

```sh
cd sentinel-dashboard
```

将会进入 dashboard 模块，然后在 dashboard 目录下执行：

```sh
mvn clean package
```

maven将会把 sentinel-dashboard 模块打包成一个可执行的 fat jar包，如下图所示：

![sentinel-dashboard-module-2](images/sentinel-dashboard-module-2.png)

![sentinel-dashboard-module-3](images/sentinel-dashboard-module-3.png)

### 启动控制台

构建成功后，就可以启动控制台了，执行以下命令：

```sh
java -Dserver.port=8080 \
-Dcsp.sentinel.dashboard.server=localhost:8080 \
-jar target/sentinel-dashboard.jar
```

其中 `-Dserver.port=8080` 用于指定 Sentinel 控制台端口为 `8080`。

执行完之后，你将看到如下信息：

![sentinel-dashboard-start-1](images/sentinel-dashboard-start-1.png)

![sentinel-dashboard-start-2](images/sentinel-dashboard-start-2.png)

当看到 `Started DashboardApplication in xx seconds` 时，说明你的控制台已经启动成功了，访问 [http://localhost:8080/](http://localhost:8080/) 就可以看到控制台的样子了，如下图所示：

![sentinel-dashboard-1](images/sentinel-dashboard-1.png)

可以看到当前控制台中没有任何的应用，因为还没有应用接入。



## 接入控制台

要想在控制台中操作我们的应用，除了需要部署一个控制台的服务外，还需要将我们的应用接入到控制台中去。

### 引入 transport 依赖

首先需要在我们使用 sentinel 的服务中引入 sentinel-transport 的依赖，因为我们的应用是作为客户端，通过transport模块与控制台进行通讯的，依赖如下所示：

```xml
<dependency>
    <groupId>com.alibaba.csp</groupId>
    <artifactId>sentinel-transport-simple-http</artifactId>
    <version>x.y.z</version>
</dependency>
```

版本依然选择最新的 1.4.0

### 配置应用启动参数

引入了依赖之后，接着就是在我们的应用中配置 JVM 启动参数，如下所示：

```sh
-Dproject.name=xxx -Dcsp.sentinel.dashboard.server=consoleIp:port
```

其中的consoleIp和port对应的就是我们部署的 sentinel dashboard 的ip和port，我这里对应的是 127.0.0.1 和 8080，按照实际情况来配置 dashboard 的ip和port就好了，如下图所示：

![sentinel-connection-properties](images/sentinel-connection-properties.png)

从图中可以看到我设置的客户端的应用名为：lememo，当客户端连接上控制台后，会显示该应用名。

PS：需要注意的是，除了可通过 JVM -D 参数指定之外，也可通过 properties 文件指定，配置文件的路径为 `${user_home}/logs/csp/${project.name}.properties`。

配置文件中参数的key和类型如下所示：

![sentinel-dashboard-properties](images/sentinel-dashboard-properties.png)

优先级顺序：JVM -D 参数的优先级最高，若 properties 文件和 JVM 参数中有相同项的配置，以 JVM -D 参数配置的为准。

### 触发客户端连接控制台

客户端配置好了与控制台的连接参数之后，并不会主动连接上控制台，需要触发一次客户端的规则才会开始进行初始化，并向控制台发送心跳和客户端规则等信息。

客户端与控制台的连接初始化是在 Env 的类中触发的，即下面代码中的 `InitExecutor.doInit();`：

```java
public class Env {
    public static final NodeBuilder nodeBuilder = new DefaultNodeBuilder();
    public static final Sph sph = new CtSph();

    static {
        // If init fails, the process will exit.
        InitExecutor.doInit();
    }
}
```

### 埋点

上篇文章中我们创建了一个 UserService 来做验证，正常时会返回一个用户对象，被限流时返回一个null，但是这样不太直观，本篇文章我换一个更简单和直观的验证方式，代码如下所示：

```java
@GetMapping("/testSentinel")
public @ResponseBody
String testSentinel() {
    String resourceName = "testSentinel";
    Entry entry = null;
    String retVal;
    try{
        entry = SphU.entry(resourceName,EntryType.IN);
        retVal = "passed";
    }catch(BlockException e){
        retVal = "blocked";
    }finally {
        if(entry!=null){
            entry.exit();
        }
    }
    return retVal;
}
```

**PS：这里有个需要注意的知识点，就是 SphU.entry 方法的第二个参数 EntryType 说的是这次请求的流量类型，共有两种类型：IN 和 OUT 。**

**IN**：是指进入我们系统的入口流量，比如 http 请求或者是其他的 rpc 之类的请求。

**OUT**：是指我们系统调用其他第三方服务的出口流量。

入口、出口流量只有在配置了系统规则时才有效。

设置 Type 为 IN 是为了统计整个系统的流量水平，防止系统被打垮，用以自我保护的一种方式。

设置 Type 为 OUT 一方面是为了保护第三方系统，比如我们系统依赖了一个生成订单号的接口，而这个接口是核心服务，如果我们的服务是非核心应用的话需要对他进行限流保护；另一方面也可以保护自己的系统，假设我们的服务是核心应用，而依赖的第三方应用老是超时，那这时可以通过设置依赖的服务的 rt 来进行降级，这样就不至于让第三方服务把我们的系统拖垮。

下图描述了流量的类型和系统之间的关系：

![sentinel-entry-type](images/sentinel-entry-type.png)



### 连接控制台

应用接入 transport 模块之后，我们主动来访问一次 `/testSentinel` 接口，顺利的话，客户端会主动连接上控制台，并将自己的ip等信息发送给控制台，并且会与控制台维持一个心跳。

现在我们在来访问下控制台，看到客户端已经连接上来了，如下图所示：

![sentinel-dashboard-2](images/sentinel-dashboard-2.png)

客户端连接上dashboard之后，我们就可以为我们定义的资源配置规则了，有两种方式可以配置规则：

- 在【**流控规则**】页面中新增
- 在【**簇点链路**】中添加

我们可以在【流控规则】页面中新增，点击【流控规则】进入页面，如下图所示：

![sentinel-add-flow-rule-1](images/sentinel-add-flow-rule-1.png)

在弹出框中，填写资源名和单机阈值，其他的属性保持默认设置即可，如下图所示：

![sentinel-add-flow-rule-2](images/sentinel-add-flow-rule-2.png)

点击【新增】后，规则即生效了。

第二种方式就是在【簇点链路】的页面中找到我们埋点的资源名，然后直接对该资源进行增加流控规则的操作，如下图所示：

![sentinel-add-flow-rule-3](images/sentinel-add-flow-rule-3.png)

上图中右侧的【+流控】的按钮点击后，弹出框与直接新增规则是一样的，只是会自动将资源名填充进去，省去了我们设置的这一步。

### 验证效果

规则创建完成之后，我们就可以在【流控规则】页面查询到了，如下图所示：

![sentinel-flow-rule-list](images/sentinel-flow-rule-list.png)

接着我们就可以来验证效果了，让我们在浏览器中快速的刷新来请求 `/testSentinel` 这个接口，不出意外，应该会看到如下图所示的情况：

![sentinel-flow-rule-effect-1](images/sentinel-flow-rule-effect-1.png)

说明我们设置的流控规则生效了，请求被 block 了。

现在我们再到控制台的【实时监控】页面查询下，刚刚我们的一顿疯狂请求应该有很多都被 block 了，通过的 qps 应该维持在2以下，如下图所示：

![sentinel-flow-rule-effect-2](images/sentinel-flow-rule-effect-2.png)



## 原理

我们知道 sentinel 的核心就是围绕着几件事：资源的定义，规则的配置，代码中埋点。

而且这些事在 sentinel-core 中都有能力实现，也对外暴露了相应的 http 接口方便我们查看 sentinel 中的相关数据。

### CommandCenter

sentinel-core 在第一次规则被触发的时候，会通过spi扫描的方式启动一个并且仅启动一个 CommandCenter，也就是我们引入的 sentinel-transport-simple-http 依赖中被引入的实现类：SimpleHttpCommandCenter。

这个 SimpleHttpCommandCenter 类中启动了两个线程池：主线程池和业务线程池。

主线程池启动了一个 ServerSocket 来监听默认的 8719 端口，如果端口被占用，会自动尝试获取下一个端口，尝试3次。

业务线程池主要是用来处理 ServerSocket 接收到的数据。

将不重要的代码省略掉之后，具体的代码如下所示：

```java
public class SimpleHttpCommandCenter implements CommandCenter {
	// 省略初始化
    private ExecutorService executor;
	private ExecutorService bizExecutor;

	@Override
	public void start() throws Exception {

	    Runnable serverInitTask = new Runnable() {
	        int port;
	        {
	            try {
	                port = Integer.parseInt(TransportConfig.getPort());
	            } catch (Exception e) {
	                port = DEFAULT_PORT;
	            }
	        }

	        @Override
	        public void run() {
	        	// 获取可用的端口用以创建一个ServerSocket
	            ServerSocket serverSocket = getServerSocketFromBasePort(port);
	            if (serverSocket != null) {
	            	// 在主线程中启动ServerThread用以接收socket请求
	                executor.submit(new ServerThread(serverSocket));
	                // 省略部分代码
	            } else {
	                CommandCenterLog.info("[CommandCenter] chooses port fail, http command center will not work");
	            }
	            executor.shutdown();
	        }
	    };
	    new Thread(serverInitTask).start();
	}

	class ServerThread extends Thread {
        private ServerSocket serverSocket;

        ServerThread(ServerSocket s) {
            this.serverSocket = s;
        }

        @Override
        public void run() {
            while (true) {
                Socket socket = null;
                try {
                    socket = this.serverSocket.accept();
                    setSocketSoTimeout(socket);
                    // 将接收到的socket封装到HttpEventTask中由业务线程去处理
                    HttpEventTask eventTask = new HttpEventTask(socket);
                    bizExecutor.submit(eventTask);
                } catch (Exception e) {
                    // 省略部分代码
                }
            }
        }
    }
}
```

具体的情况如下图所示：

![command-center](images/command-center.png)



### HTTP接口

SimpleHttpCommandCenter 启动了一个 ServerSocket 来监听8719端口，也对外提供了一些 http 接口用以操作 sentinel-core 中的数据，包括查询|更改规则，查询节点状态等。

**PS：控制台也是通过这些接口与 sentinel-core 进行数据交互的！**

提供这些服务的是一些 CommandHandler 的实现类，每个类提供了一种能力，这些类是在 sentinel-transport-common 依赖中提供的，如下图所示：

![command-handler](images/command-handler.png)



#### 查询规则

运行下面命令，则会返回现有生效的规则：

```
curl http://localhost:8719/getRules?type=<XXXX>
```

其中，type有以下取值：

- `flow` 以 JSON 格式返回现有的限流规则；
- `degrade` 则返回现有生效的降级规则列表；
- `system` 则返回系统保护规则。

#### 更改规则

同时也可以通过下面命令来修改已有规则：

```
curl http://localhost:8719/setRules?type=<XXXX>&data=<DATA>
```

其中，type 可以输入 `flow`、`degrade` 等方式来制定更改的规则种类，`data` 则是对应的 JSON 格式的规则。

其他的接口不再一一详细举例了，有需要的大家可以自行查看源码了解。



## 我的公众号

如果你觉得该项目对您有帮助，欢迎您关注我的公众号「逅弈逐码」，了解更多原创文章。

![logo](../../logo.jpg)

