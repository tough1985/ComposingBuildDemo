# Android源码依赖与自动化提交

## 1.背景

我们在日常的业务开发中，经常会遇到这种情况，组件化的业务项目，众多的业务组件以及工具组件，众多的组件造成我们编译运行一次相当耗时，慢慢的，我们将基础公共工具组件放到maven库中，通过maven依赖的方式引入到业务中，但是如果尝试修改某个工具组件的某个功能并且想在当前的项目中调试就会非常复杂而且成本较高。

以修改一个底层工具库举例，通常的做法是修改底层工具类库的代码，如果工具库有运行环境，一般会在工具库的运行环境中看一下修改的运行结果，然后提交到maven，项目再来修改对该工具库最新版本的依赖，再次运行查看效果，如果效果不满意，还要在重复一次上面的操作。另外经常还遇到一些开发不规范的小伙伴们，只上传了maven最新的版本，但是代码忘记提交这种情况，使后面维护的小伙伴一脸懵逼。

而我们理想化的状态是，我们在写某个业务需求的时候，只编译当前业务的组件，其他的业务组件或者公共工具组件使用maven依赖的方式引入，这样可以降低整体的编译时间，但是如果需要调试到其他的业务或者公共组件的时候，我们希望该组件能以源码的方式引入，并且可以在当前的项目运行环境中debug调试，修改源文件，与当前的项目一起编译运行提升我们的开发效率。同时在开发完成时候，我们可以自动提交代码并上传到maven，项目也自动的依赖最新的该组件的最新版本。

所以本文意在解决如下问题：

1. 动态切换本地源码依赖与maven依赖
2. 自动提交代码并上传到maven

## 2. 动态切换本地源码依赖与Maven依赖

### 2.1 项目搭建

我们用两个项目来模拟实际的业务组件与工具组件的关系。

首先创建*ComposingBuildDemo*目录，在该目录下创建两个项目

- ComposingBuildApp 用来模拟业务组件
- ComposingBuildLibrary 用来模拟工具库组件


在*ComposingBuildLibrary*项目中创建一个module，命名为*composinglibrary*

目前的结构状态如下

```
ComposingBuildDemo
├── ComposingBuildApp
└── ComposingBuildLibrary
    └── composinglibrary
```

为了方便大家理解，我在github上面创建了两个项目，大家最好fork到自己的github，这样方便自己的测试和修改，因为后面涉及到git的提交。

[ComposingBuildDemo](https://github.com/tough1985/ComposingBuildDemo)

clone下来项目以后，将tag切换到step1，即是本章节的代码示例。

```
git checkout step1
```


### 2.2 includeBuild初试

要使用源码的方式引入组件，我们可以使用Gradle提供的一个功能*Composing builds*。

感兴趣的同学可以先看一遍官方的说明[Composing builds](https://docs.gradle.org/current/userguide/composite_builds.html)，不过官方的说明并没有具体的例子，稍微有点点晦涩。简单的解释一下，通常使用的Gradle是一个项目里面有多个子项目，只有根项目中有一个*setting.gradle*文件，这种成为多项目构建，而*Composing builds*是可以在一个完成的gradle项目中，引入另一个完整gradle项目（也就是具有setting.gradle文件的完整），从而去依赖这个项目的构建结果，称为复合构建。

使用复合构建也非常的简单，以上一个章节的两个项目为例。

本节项目代码：

[ComposingBuildDemo](https://github.com/tough1985/ComposingBuildDemo)


将tag切换到step2，即是本章节的代码示例。

#### 2.2.1 配置ComposingBuildLibrary项目

首先修改*ComposingBuildLibrary*这个项目，在*composinglibrary*这个module的*build.gradle*文件中添加代码：

```groovy
// 配置group和version 使项目可以用本地依赖的方式通过group + 项目名称 + version的方式进行依赖
group "me.xiba.lib"
version "1.0"
```

为了简单的测试效果，在*composinglibrary*中添加一个类文件，代码如下：

```java
public class Constant {
    public static final String TAG = "ComposingLibrary";
}
```

在这定义一个常量，让*ComposingBuildApp*能够应用到这个常量，就表示已经添加了*composinglibrary*这个module的依赖。

#### 2.2.2 在ComposingBuildApp项目中添加includeBuild

接下来修改*ComposingBuildApp*这个项目，在*ComposingBuildApp*项目的*setting.gradle*文件中，添加代码:

```groovy
// 通过includeBuild可以引用到ComposingBuildLibrary项目，参数是ComposingBuildLibrary与当前项目的相对路径
includeBuild("../ComposingBuildLibrary")
```

Sync之后，可以看到项目中已经添加了*ComposingBuildLibrary*的源码。

![WechatIMG239.png](http://ww1.sinaimg.cn/large/75ddb715gy1gitnzghyflj20sg09g3zv.jpg)

#### 2.2.3 在ComposingBuildApp项目中引入composinglibrary模块

编辑ComposingBuildApp项目中app目录下的*build.gradle*文件，在dependencies中添加依赖：

```groovy
dependencies {
    ......

    // 通过setting.gradle 配置的 includeBuild 可以引用到composingLibrary的aar
   implementation 'me.xiba.lib:composinglibrary:1.0'

    ......

}
```

编辑ComposingBuildApp项目中app目录下的*MainActivity*文件，尝试引用*Constant.TAG*，如果引用成功，则表示本地依赖成功。代码如下：

```java
public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // 引入ComposingBuildLibrary的Constant.TAG
        ((TextView)findViewById(R.id.tv_content)).setText(Constant.TAG);
    }
}
```

### 2.3 本地maven搭建

现在我们已经可以通过*includeBuild*使用源码依赖进行符合构建，接下来要实现maven依赖和本地依赖之间的切换。为了模拟maven，我们先将*ComposingBuildLibrary*项目发布到本地maven。

本节项目代码：

[ComposingBuildDemo](https://github.com/tough1985/ComposingBuildDemo)


将tag切换到step3，即是本章节的代码示例。

#### 2.3.1 添加ComposingBuildLibrary项目对Maven的配置

在*composinglibrary*这个module的*build.gradle*文件中添加代码：

```java
apply plugin: 'maven'

······

// 配置项目上传Maven的相关信息
uploadArchives {
    repositories.mavenDeployer {
        // 本地仓库路径，项目根目录下的 repo 的文件夹为例
        repository(url: uri('../../repo'))

        // groupId
        pom.groupId = 'me.xiba.lib'

        // artifactId 为了与本地依赖区别，添加Maven后缀
        pom.artifactId = 'composinglibraryMaven'

        // 版本号
        pom.version = '1.0'
    }
}

```

sync之后，可以在右侧的gradle中，在composinglibrary/tasks/upload中，找到uploadArchives任务，双击执行，执行之后可以发现*composingBuild*根目录下，多了一个*repo*目录。

#### 2.3.2 使用maven来引入ComposingBuildLibrary

首先修改*ComposingBuildApp*项目的*setting.gradle*文件，将includeBuild("../ComposingBuildLibrary")语句注释掉。代码如下：

```groovy
// 通过includeBuild可以引用到ComposingBuildLibrary项目，参数是ComposingBuildLibrary与当前项目的相对路径
//includeBuild("../ComposingBuildLibrary")
```

编辑*ComposingBuildApp*项目中app目录下的*build.gradle*文件，添加本地仓库的引用，同时修改dependencies，使用maven的方式引入*ComposingBuildLibrary*。代码如下：

```groovy

// 添加本地Maven仓库
repositories {
    maven { url uri('../../repo') }
}

dependencies {
    ......

    // 通过setting.gradle 配置的 includeBuild 可以引用到composingLibrary的aar
//    implementation 'me.xiba.lib:composinglibrary:1.0'

    // 通过Maven引用composingLibrary的aar
    implementation 'me.xiba.lib:composinglibraryMaven:1.0'

    ......

}
```

sync之后，*ComposingBuildLibrary*项目的源码已经不在本地了，但是MainActivity依然可以引用到**Constant.TAG**，这时已经使用Maven进行依赖编译了。


### 2.4 添加substitute对maven依赖进行替换

虽然现在可以实现了源码依赖和Maven依赖，但是每次切换的时候，都要注释掉对应的代码，然后打开另外一部分代码，如果多个组件使用了同一个库，那么这个操作会更加麻烦。

接下来我们使用substitute特性用源码替换maven依赖。

本节项目代码：

[ComposingBuildDemo](https://github.com/tough1985/ComposingBuildDemo)


将tag切换到step4，即是本章节的代码示例。


修改*ComposingBuildApp*项目的*setting.gradle*文件，代码如下：

```groovy
// 通过includeBuild可以引用到ComposingBuildLibrary项目，参数是ComposingBuildLibrary与当前项目的相对路径
// with project为"ComposingBuildLibrary"中的"composinglibrary"项目
includeBuild("../ComposingBuildLibrary") {
    dependencySubstitution {
        substitute module('me.xiba.lib:composinglibraryMaven') with project(':composinglibrary')
    }
}
```

sync之后，*ComposingBuildLibrary*项目回到了本地，MainActivity所引用的**Constant.TAG**是本地的源码，但是*ComposingBuildApp*项目的build.gradle文件中添加的确实maven的项目地址，这就是substitue的一个功能。

### 2.5 添加配置来动态切换本地依赖与Maven依赖

目前为止，我们能够将本地源码替换Maven依赖来编译项目，但是如果想要切换的话，需要不断的重复注释掉*settings.gradle*里面所对应的设置，如果项目过多的话，*settings.gradle*会显得冗长杂乱，这里我们选择使用通过配置文件的方式来对项目进行配置，然后通过在*settings.gradle*文件中添加一小段代码来动态添加**includeBuild**语句。

本节项目代码：

[ComposingBuildDemo](https://github.com/tough1985/ComposingBuildDemo)

将tag切换到step5，即是本章节的代码示例。

#### 2.5.1 添加配置文件 

在*ComposingBuildApp*项目中添加**composingConfig.gradle**文件，内容如下：

```groovy
ext {
    composing_dependencies = [
            composing_library = [
                    isLocal : true,    // 是否本地依赖
                    projectPath : "../ComposingBuildLibrary",   // 项目的路径
                    projectName : ":composinglibrary",          // 项目名称
                    projectMaven : "me.xiba.lib:composinglibraryMaven"      // 项目的maven地址
            ]
    ]
}
```

在文件中对项目相关的信息进行配置，如果是本地源码依赖，就将**isLocal**修改为true，maven依赖为false。

#### 2.5.2 在settings.gradle中读取配置

接下来修改*ComposingBuildApp*项目中的**settings.gradle**文件，内容如下：

```groovy
include ':app'
rootProject.name = "ComposingBuildApp"

// 引入composing的设置
apply from: "composingConfig.gradle"

//// 通过includeBuild可以引用到ComposingBuildLibrary项目，参数是ComposingBuildLibrary与当前项目的相对路径
//// with project为"ComposingBuildLibrary"中的"composinglibrary"项目
//includeBuild("../ComposingBuildLibrary") {
//    dependencySubstitution {
//        substitute module('me.xiba.lib:composinglibraryMaven') with project(':composinglibrary')
//    }
//}

// 遍历composing_dependencies中的所有配置
ext.getProperty("composing_dependencies").each { projectConfig ->

    // 如果是本地依赖
    if (projectConfig["isLocal"]){

        // 使用本地依赖进行替换
        includeBuild(projectConfig["projectPath"]) {
            dependencySubstitution {
                substitute module(projectConfig["projectMaven"]) with project(projectConfig["projectName"])
            }
        }
    }
}
```

首先引入之前添加的**composingConfig.gradle**文件，遍历**composing_dependencies**属性下的所有设置，如果项目被设置为本地依赖，那么添加includeBuild的相关语句，使用源码替换本地依赖。

如果想让项目为本地源码依赖，将对象项目的**isLocal**设置为**true**，不过要**Sync**之后才能生效。

## 3. 自动化git提交与Maven上传

开发过程中有的时候会因为操作不规范，出现git仓库的代码与maven不一致的问题，出现的原因大部分是因为提交了代码但是没有上传到Maven，或者上传了Maven但是没有提交代码的问题。本章节主要完成一键提交git并上传Maven的功能。

先来分析一下思路：
1. 通常提交git要敲多次命令，除了commit message，其他的操作基本是重复的，那么可以通过编写一个脚本来完成整个git的提交
2. 由于Maven的上传是通过gradle的maven插件提供的**uploadArchives**任务来完成的，既然是Gradle Task，那么我们可以将上面的git提交脚本封装成一个task，在将两个task关联起来。

### 3.1 编写git提交脚本
本节项目代码：

[ComposingBuildDemo](https://github.com/tough1985/ComposingBuildDemo)

将tag切换到step6，即是本章节的代码示例。

在*ComposingBuildLibrary*目录下添加一个shell文件，命名为**gitcommit.sh**，内容如下：

```shell
#!/bin/bash

# 传递参数

if [ ! $1 ]
then
    echo "########## 请输入提交信息 ##########"
    exit 1;
fi

echo "########## 开始提交 ##########"

echo "commitMessage: $1"

git add -A

# 获取git status的结果
statusResult=`git status`

# 如果返回内容包含'nothing to commit'说明没有要返回的内容，直接返回
if [[ $statusResult == *"nothing to commit"* ]]
then
  echo "########## 没有需要提交的内容 ##########"
  exit 1
fi

echo "########## 请输入提交信息 ##########"

git commit -m "$1"

if [ $? -ne 0 ]
then
  echo "git commit 错误"
  exit 1
fi

git fetch
if [ $? -ne 0 ]
then
  echo "git fetch 错误"
  exit 1
fi

git rebase
if [ $? -ne 0 ]
then
  echo "git rebase 错误"
  exit 1
fi

git push -u origin
if [ $? -ne 0 ]
then
  echo "git push 错误"
  exit 1
fi

echo "########## 提交结束 ##########"
```

shell主要是接受了一个参数作为提交用的CommitMessage，这里使用的是fetch，rebase的流程，如果使用其他的流程，可以自行修改脚本的内容。

执行脚本，需要在终端中进入到*composingBuildLibrary*目录下，输入:

```shell
bash gitcommit.sh "commitMessage"
```

### 3.2 编写Gradle task执行脚本

本节项目代码：

[ComposingBuildDemo](https://github.com/tough1985/ComposingBuildDemo)

将tag切换到step7，即是本章节的代码示例。

编辑*ComposingBuildLibrary/composinglibrary*项目的build.gradle文件，添加一个task来执行shell脚本，代码如下：

```groovy

android {
    ······
}

// 自动提交代码的task
task gitcommit(type: Exec){

    description("git push task")

    doFirst {
        println "gitpush task running"
        // 执行gitcommit.sh脚本
        commandLine("bash", "../gitcommit.sh", "commitMessage")
    }

    doLast(){
        println("gitpush task done")
    }
}

dependencies {
    ······
}
```

这里有个细节要注意，task的type要使用Exec类型，[点这里查看Exec API](https://docs.gradle.org/current/dsl/org.gradle.api.tasks.Exec.html)，Exec内部封装了执行commandLine的操作，同时他也提供了切换工作目录和获取输出结果的功能。

sync之后，可以在右侧Gradle任务列表中找到gitcommit任务，具体的路径为：ComposingBuildLibrary/composinglibrary/Tasks/other/gitcommit。双击执行任务，即可出发提交。当然也可以通过命令行的方式执行，命令如下：

```shell
./gradlew gitcommit
```

在执行任务的过程中，commandLine的最后一个参数是提交信息，每次提交的时候都需要修改commitMessage，但是由于修改build.gradle会出发sync，因此这里我们使用properties文件的方式来处理commitMessage，以避免出发build.gradle的sync。

在*ComposingBuildLibrary/composinglibrary*项目下添加文件，命名为**gitcommit.properties**，内容如下：

```shell
commitMessage=add gitcommit.properties
```

接下来修改*gitcommit task*的代码

```groovy

android {
    ······
}

// 自动提交代码的task
task gitcommit(type: Exec){

    description("git push task")

    doFirst {
        println "gitpush task running"
        // 读取配置文件
        def composingProperties = new Properties()
        composingProperties.load(new FileInputStream(file("./gitcommit.properties")))

        // 执行gitcommit.sh脚本
        commandLine("bash", "../gitcommit.sh", composingProperties.get("commitMessage"))
    }

    doLast(){
        println("gitpush task done")
    }
}

dependencies {
    ······
}
```

主要是添加了对*gitcommit.properties*文件的加载，并读取**commitMessage**属性作为commandLine的参数，这样每次提交就避免了引发gradle sync。

### 3.3 在上传Maven前执行commit

本节项目代码：

[ComposingBuildDemo](https://github.com/tough1985/ComposingBuildDemo)

将tag切换到step8，即是本章节的代码示例。

现在我们有了可以提交git的Gradle task，那么如何将他与Maven的uploadArchives任务关联起来呢？

这里介绍两种方式，一种是使用**dependsOn**，另外一种是使用**mustRunAfter**。

先贴出代码，编辑*ComposingBuildLibrary/composinglibrary*项目的build.gradle文件，内容如下：

```groovy

android {
    ······
}

// 自动提交代码的task
task gitcommit(type: Exec){

    description("git push task")

    doFirst {
        println "gitpush task running"
        // 读取配置文件
        def composingProperties = new Properties()
        composingProperties.load(new FileInputStream(file("./gitcommit.properties")))

        // 执行gitcommit.sh脚本
        commandLine("bash", "../gitcommit.sh", composingProperties.get("commitMessage"))
    }

    doLast(){
        println("gitpush task done")
    }
}

// 通过taskName获取uploadArchives任务
Task uploadTask = project.tasks.getByName("uploadArchives")

// 如果用depensOn，那么uploadArchives每次执行前都会先执行gitcommit，无法独立运行
uploadTask.dependsOn(gitcommit)

// 如果想要uploadArchives更加独立，也可以使用shouldRunAfter，这样提交和上传两个task完全是独立的
// 但是如果同时执行两个task的时候，uploadArchives会在commit任务执行完成之后执行。
// 在终端输入 ./gradlew gitcommit uploadArchives
uploadTask.mustRunAfter(gitcommit)

dependencies {
    ······
}
```

如果是使用**dependsOn**的话，代码为：

```groovy
uploadTask.dependsOn(gitcommit)
```

这句代码的意思是，uploadTask每次执行的时候，都会先执行gitcommit。uploadTask无法独立运行，但是gitcommit可以独立运行。

使用**dependsOn**的方式，直接运行**uploadArchives**

如果是使用**mustRunAfter**的话，代码为：

```groovy
uploadTask.mustRunAfter(gitcommit)
```

这句代码的意思是，uploadTask可以单独运行，gitcommit也可以单独运行，但是如果同时执行两个的话，那么uploadTask必须在gitCommit执行完成之后再执行。

使用**mustRunAfter**的方式，需要在终端输入：

```shell
./gradlew gitcommit uploadArchives
```

两种方式可以按照需求二选一。

### 3.4 将commit task封装为插件

目前我们虽然打通了主流程的功能，但是还是有一些细节没有处理，比如说如果没有gitcommit.properties文件怎么办？如果gitcommit执行出错，但是依然上传了maven怎么办？如果想要在所有组件都添加gitcommit，是不是要不断的复制粘贴代码？

为了解决上述问题，本节我们来编辑一个Gradle Plugin来解决。


#### 3.4.1 创建Gradle Plugin

为了避免每个项目都贴一份相同的代码，我们开发一个gradle plugin，在插件中创建对应的提交task。

创建Gradle Plugin有几种方式，为了方便演示，这里选择使用buildSrc的方式创建Gradle Plugin。当然大家也可以选择创建一个独立的Gradle Plugin项目。

关于如何创建编写一个Gradle Plugin，可以参考官方的文档[Developing Custom Gradle Plugins](https://docs.gradle.org/current/userguide/custom_plugins.html)。

在*ComposingBuildLibrary*项目中创建*buildSrc*目录，在*buildSrc*目录中添加*git_commit_plugin*目录、*build.gradle*文件、*setting.gradle*文件。

**setting.gradle**文件内容：

```groovy
// buildSrc中，文件目录的名称
include "git_commit_plugin"
```

**build.gradle**文件内容：

```groovy
subprojects {
    apply plugin: "groovy"
    apply plugin: 'java-gradle-plugin'
    apply plugin: 'kotlin'

    sourceCompatibility = "1.8"
    targetCompatibility = "1.8"

    dependencies {
        implementation localGroovy()
        implementation gradleApi()

        implementation "org.jetbrains.kotlin:kotlin-stdlib-jdk8:1.3.72"
        implementation 'com.android.tools.build:gradle:4.0.1'

    }

    rootProject.dependencies {
        runtime project(path)
    }
}

allprojects {
    repositories {
        google()
        jcenter()
    }
}

apply plugin: 'java-gradle-plugin'
apply plugin: 'kotlin'

buildscript {
    ext.kotlin_version = '1.3.72'
    repositories {
        mavenCentral()
    }
    dependencies {
        classpath "org.jetbrains.kotlin:kotlin-gradle-plugin:$kotlin_version"
    }
}

repositories {
    mavenCentral()
}

dependencies {
    compileOnly gradleApi()
    compileOnly localGroovy()
    implementation "org.jetbrains.kotlin:kotlin-stdlib-jdk8:$kotlin_version"
}
```

由于代码是使用的kotlin，所以加了很多kotlin引入相关的配置。

*composing_plugin*目录下主要有两部分，一个是源码部分，一个是resources的配置部分。

由于使用的是kotlin进行编辑，所以源码部分创建目录/src/main/java，在java目录下创建包/me/xiba/gitcommit/plugin，在包下添加kt文件

- GitCommitPlugin：用于在项目的build.gradle中添加gitcommit任务
后面我们在来介绍文件的内容。

而resources的目录有一点点讲究，要在/main目录下创建/resources/META-INF/gradle-plugins/me.xiba.gitcommit.properties文件，其中**me.xiba.gitcommit**就是引入插件时使用的名称，使用**apply plugin: "me.xiba.gitcommit"** 就可以使用该插件。

**me.xiba.gitcommit.properties**文件内容如下：

```shell
implementation-class=me.xiba.gitcommit.plugin.GitCommitPlugin
```

整体的项目结构如图：

![WechatIMG249.png](http://ww1.sinaimg.cn/large/75ddb715gy1gjrbhycolsj20gv0jfjt3.jpg)

#### 3.4.2 添加文件模板

如果一个项目引入了插件但是没有创建相关的**gitcommit.properties**文件和**gitcommit.sh**文件，会有些尴尬，为了避免此类场景，我们在gradle的准备阶段，检测项目中是否有相关的文件，如果没有，使用模板创建相关的文件。

首先创建包/me/xiba/gitcommit/filetemplate，在包下创建两个文件分别为**GitCommitShellTemplate**和**GitCommitPropertiesTemplate**。

- GitCommitShellTemplate：gitcommit.sh文件的模板
- GitCommitPropertiesTemplate：gitcommit.properties文件的模板

**GitCommitShellTemplate**的代码如下：

```kotlin
package me.xiba.gitcommit.filetemplate

/**
 * @Description: gitcommit.sh模板文本
 */
class GitCommitShellTemplate {

    companion object {
        const val GIT_COMMIT_SHELL_TEMPLATE = "#!/bin/bash\n" +
                "\n" +
                "# 传递参数\n" +
                "\n" +
                "if [ ! \$1 ]\n" +
                "then\n" +
                "    echo \"########## 请输入提交信息 ##########\"\n" +
                "    exit 1;\n" +
                "fi\n" +
                "\n" +
                "echo \"########## 开始提交 ##########\"\n" +
                "\n" +
                "echo \"commitMessage: \$1\"\n" +
                "\n" +
                "git add -A\n" +
                "\n" +
                "# 获取git status的结果\n" +
                "statusResult=`git status`\n" +
                "\n" +
                "# 如果返回内容包含'nothing to commit'说明没有要返回的内容，直接返回\n" +
                "if [[ \$statusResult == *\"nothing to commit\"* ]]\n" +
                "then\n" +
                "  echo \"########## 没有需要提交的内容 ##########\"\n" +
                "  exit 1\n" +
                "fi\n" +
                "\n" +
                "echo \"########## 请输入提交信息 ##########\"\n" +
                "\n" +
                "git commit -m \"\$1\"\n" +
                "\n" +
                "if [ \$? -ne 0 ]\n" +
                "then\n" +
                "  echo \"git commit 错误\"\n" +
                "  exit 1\n" +
                "fi\n" +
                "\n" +
                "git fetch\n" +
                "if [ \$? -ne 0 ]\n" +
                "then\n" +
                "  echo \"git fetch 错误\"\n" +
                "  exit 1\n" +
                "fi\n" +
                "\n" +
                "git rebase\n" +
                "if [ \$? -ne 0 ]\n" +
                "then\n" +
                "  echo \"git rebase 错误\"\n" +
                "  exit 1\n" +
                "fi\n" +
                "\n" +
                "git push -u origin\n" +
                "if [ \$? -ne 0 ]\n" +
                "then\n" +
                "  echo \"git push 错误\"\n" +
                "  exit 1\n" +
                "fi\n" +
                "\n" +
                "echo \"########## 提交结束 ##########\""
    }
}
```

**GitCommitPropertiesTemplate**的代码如下：

```kotlin
package me.xiba.gitcommit.filetemplate

/**
 * @Description: gitcommit.properties模板文本
 */
class GitCommitPropertiesTemplate {

    companion object{
        const val GIT_COMMIT_PROPERTIES_TEMPLATE = "#\n" +
                "commitMessage=edit your commit message"
    }
}
```

接下来开始编写插件，这里直接贴出**GitCommitPlugin**的代码:

```kotlin
package me.xiba.gitcommit.plugin

import me.xiba.gitcommit.filetemplate.GitCommitPropertiesTemplate.Companion.GIT_COMMIT_PROPERTIES_TEMPLATE
import me.xiba.gitcommit.filetemplate.GitCommitShellTemplate.Companion.GIT_COMMIT_SHELL_TEMPLATE
import org.gradle.api.Plugin
import org.gradle.api.Project
import java.io.BufferedWriter
import java.io.File
import java.io.OutputStreamWriter

/**
 * @Description: git提交插件
 */
class GitCommitPlugin : Plugin<Project> {

    companion object{
        const val FILENAME_GIT_COMMIT_SHELL = "gitcommit.sh"
        const val FILENAME_GIT_COMMIT_PROPERTIES = "gitcommit.properties"

    }

    override fun apply(project: Project) {

        println("CommitUploadPlugin: ---------------------")

        // 判断是否有gitcommit.sh，如果没有创建文件
        // 由于shell文件基本一致，只需要生成一份即可，所以生成在 项目的根目录下
        var gitCommitFile = File("${project.rootProject.projectDir}/$FILENAME_GIT_COMMIT_SHELL")
        checkAndGenerateFile(gitCommitFile, GIT_COMMIT_SHELL_TEMPLATE, FILENAME_GIT_COMMIT_SHELL)

        // 判断是否有gitcommit.properties，如果没有创建文件
        // 由于项目使用的是同一个代码仓库，所以也可以使用同一个gitcommit.properties文件
        // 可自行按照需求确定位置
        var gitCommitProperties = File("${project.rootProject.projectDir}/$FILENAME_GIT_COMMIT_PROPERTIES")
        checkAndGenerateFile(gitCommitProperties, GIT_COMMIT_PROPERTIES_TEMPLATE, FILENAME_GIT_COMMIT_PROPERTIES)
    }

    /**
     * 检测文件是否存在，如果不存在，用模板生成文件
     */
    private fun checkAndGenerateFile(file: File, template: String, fileName: String){

        // 判断文件是否存在，如果不存在，使用模板创建文件
        if (!file.exists()){

            println("CommitUploadPlugin: 正在创建${fileName}文件")

            // 使用模板创建文件
            file.createNewFile()

            // 为了避免中文乱码
            var gitCommitPropertiesFileWriter = BufferedWriter(
                OutputStreamWriter(
                    file.outputStream(), "UTF-8")
            )

            gitCommitPropertiesFileWriter.write(template)
            gitCommitPropertiesFileWriter.flush()
            gitCommitPropertiesFileWriter.close()
        }
    }

}
```

接下来引入插件，在/ComposingBuildLibrary/composinglibrary项目的build.gradle文件中，添加**apply plugin: 'me.xiba.gitcommit'**即可引入插件。Sync之后，如果之前没有**gitcommit.properties**和**gitcommit.sh**文件的话，会生成文件，可以先将两个文件删除，然后Sync即可生成。这里说明一下，其实使用gitcommit.sh是因为每个提交流程不太一样，而且有些提交可能还需要添加reviewer，所以为了方便配置，使用shell文本的方式，这样可以根据自己的流程修改shell的内容，但是理论上使用gradle task完全可以替代插件，只是灵活度不高。

#### 3.4.3 生成gitcommit task

GitCommitPlugin添加一个方法**createCommitTask**用来创建提交任务，代码如下：

```kotlin

    companion object{
        const val FILENAME_GIT_COMMIT_SHELL = "gitcommit.sh"
        const val FILENAME_GIT_COMMIT_PROPERTIES = "gitcommit.properties"

        const val TASKNAME_COMMIT_UPLOAD = "gitcommit"
        const val PROPERTIES_KEY_COMMIT_MESSAGE = "commitMessage"
    }

    /**
     * 创建提交任务
     * 使用命令行执行shell文件
     */
    private fun createCommitTask(project: Project): Task {
        // 创建任务
        var task = project.tasks.create("${project.name}_$TASKNAME_COMMIT_UPLOAD", Exec::class.java)
        // 任务的Group
        task.group = "GitCommit"
        // 创建输出流，用来读取命令行的输出
        var out = ByteArrayOutputStream()

        // 执行前的设置
        task.doFirst {

            // 读取properties文件，获取commitMessage
            var gitCommitPropertiesFile = File("${project.rootProject.projectDir}/$FILENAME_GIT_COMMIT_PROPERTIES")
            // 创建一个Properties
            var gitCommitProperties = Properties()

            // 如果文件存在 读取文件内容到gitCommitProperties
            if (gitCommitPropertiesFile.exists()){
                // 避免中文乱码
                gitCommitProperties.load(
                    InputStreamReader(gitCommitPropertiesFile.inputStream(), "UTF-8")
                )
            } else {
                println("You need create composing.properties file in your project dir!")
            }

            // 获取commitMessage
            var commitMessage = gitCommitProperties.getProperty(PROPERTIES_KEY_COMMIT_MESSAGE)

            // 设置工作目录
            task.workingDir(project.projectDir)
            // 设置非0 依然正常运行
            task.isIgnoreExitValue = true
            // 设置命令行输出
            task.standardOutput = out
            // 执行提交脚本
            task.commandLine("bash", FILENAME_GIT_COMMIT_SHELL, commitMessage)
        }

        // 执行后的设置
        task.doLast {
            // 获取退出值
            println("CommitUploadPlugin: ${project.name}_$TASKNAME_COMMIT_UPLOAD : 执行结果: ${task.execResult?.exitValue}")
            // 获取命令行的输出
            println("CommitUploadPlugin: ${project.name}_$TASKNAME_COMMIT_UPLOAD : 执行输出: \n\n${out.toString("UTF-8")}")

            // 如果脚本返回非0，异常退出
            if (task.execResult?.exitValue != 0){
                throw Exception("exitValue not 0")
            }
        }

        return task
    }
```

代码很简单，首先创建了一个Exec类型的task，并添加分组**GitCommit**，这样右侧的gradle的任务列表中，就会多一个**GitCommit**的分组；读取*gitcommit.properties*文件的commitMessage属性，获取提交内容，然后执行脚本。

注意这里我们通过**task.execResult?.exitValue**得到了shell执行的结果，一般0为执行成功推出，而非0则视为异常退出。在gitcommit.sh脚本中，如果操作不成功，则会使用**exit 1**的方式退出脚本，其中的'1'就是执行结果，在任务的doLast中，获取任务的执行结果，如果是非0，那么抛出异常，这样，后续的**uploadArchives**任务也不会得到执行。

最后在apply方法中调用创建任务的方法，并与**uploadArchives**任务关联：

```kotlin
override fun apply(project: Project) {

        println("CommitUploadPlugin: ---------------------")

        // 判断是否有gitcommit.sh，如果没有创建文件
        // 由于shell文件基本一致，只需要生成一份即可，所以生成在 项目的根目录下
        var gitCommitFile = File("${project.rootProject.projectDir}/$FILENAME_GIT_COMMIT_SHELL")
        checkAndGenerateFile(gitCommitFile, GIT_COMMIT_SHELL_TEMPLATE, FILENAME_GIT_COMMIT_SHELL)

        // 判断是否有gitcommit.properties，如果没有创建文件
        // 由于项目使用的是同一个代码仓库，所以也可以使用同一个gitcommit.properties文件
        // 可自行按照需求确定位置
        var gitCommitProperties = File("${project.rootProject.projectDir}/$FILENAME_GIT_COMMIT_PROPERTIES")
        checkAndGenerateFile(gitCommitProperties, GIT_COMMIT_PROPERTIES_TEMPLATE, FILENAME_GIT_COMMIT_PROPERTIES)

        // 创建提交任务
        var commitTask = createCommitTask(project)

        // 如果用depensOn，那么uploadArchives每次执行都会出发提交，无法独立运行
        project.tasks.getByName("uploadArchives").dependsOn(commitTask)

//        // 如果想要uploadArchives更加独立，也可以使用shouldRunAfter，这样提交和上传两个task完全是独立的
//        // 但是如果同时执行两个task的时候，uploadArchives会在commit任务执行完成之后执行。
//        // 在终端输入 ./gradlew composinglibrary_commitUpload uploadArchives
//        var uploadTask = project.tasks.getByName("uploadArchives")
//        uploadTask.mustRunAfter(commitTask)
    }
```

现在执行**uploadArchives**任务的时候，会自动完成提交。