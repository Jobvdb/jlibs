---
title: SAX2JavaBinding
layout: default
---

# SAX-JAVA Binding Made Easier

the package `jlibs.xml.sax.binding` contains classes which help you to read xml and create java objects.

## Dependencies ###

~~~xml
<dependency>
    <groupId>in.jlibs</groupId>
    <artifactId>jlibs-xml-binding</artifactId>
    <version>2.1</version>
</dependency> 

<dependency>
    <groupId>in.jlibs</groupId>
    <artifactId>jlibs-xml-binding-apt</artifactId>
    <version>2.1</version>
    <optional>true</optional>
</dependency> 
~~~

`jlibs-xml-binding-apt` contains annotation processor and is required only at *compile time*

## Concepts ##

Before going into details, we will first go through the concepts. Then it will be easier to understand the code.

we have employee.xml:

~~~xml
<employee>                     
    <name>scott</name>         
    <age>20</age>              
    <experience>5</experience> 
</employee>
~~~

and Employee class:

~~~java
public class Employee{
    private String name;
    private int age;
    private int experience;

    // getter and setter methods
}
~~~

Each element in xml will create a java object.
Let us see the java objects created for above xml:

~~~
        XML                     |     Java Object  
--------------------------------|-----------------
<employee>                      |   new Employee()
    <name>scott</name>          |   new String(#text) 
    <age>20</age>               |   new String(#text) 
    <experience>5</experience>  |   new String(#text) 
</employee>                     |
~~~

From above table you can see that:

- `<employe/>` element create new `Employee` object
- `<name/>`, `<age/>` and `<experience/>` elements create `String` objects with their text content(i.e `#text`)

Now we have 4 java Objects ( one `Employee` object and four `String` objects)

Now **relation** comes into picture. Each element has a relation, which tells how to relate current element's java object with parent element's java object.

~~~
        XML                     |     Java Object             |   Relation
--------------------------------|--------------------------------------------------------
<employee>                      |   emp = new Employee()      |   - No Relation -
    <name>scott</name>          |   name = new String(#text)  | emp.setName(name)
    <age>20</age>               |   age = new String(#text)   | emp.setAge(age)
    <experience>5</experience>  |   exp = new String(#text)   | emp.setExperience(exp)
</employee>                     |
~~~

The above table shows how java objects created are related with each other.  
To make understanding easier, we assigned each java object created into some variable.  

- `Employee` object created is assigned to `emp` variable
- `String` created for `<name>` element is assigned to `name` variable
- `String` created for `<age>` element is assigned to `age` variable
- `String` created for `<experience>` element is assigned to `exp` variable

Now you can see that relation of `<name>` element and its parent element `<employee>` in java is:

~~~java
emp.setName(name)
~~~

Once Java-Object and Relation are defined for each element type, It is piece of cake to read xml document into java objects.

## Implementing Binding ##

Let us put the above concepts into implementation:

~~~java
import jlibs.xml.sax.binding.*;

@Binding("employee")
public class EmployeeBinding{
    @Binding.Start
    public static Employee onStart() throws SAXException{
        return new Employee();
    }

    @Binding.Text({"name", "age", "experience"})
    public static String onText(String text){
        return text;
    }

    @Relation.Finish("name")
    public static void relateName(Employee employee, String name){
        employee.setName(name);
    }

    @Relation.Finish("age")
    public static void relateAge(Employee employee, String age){
        employee.setAge(Integer.parseInt(age));
    }

    @Relation.Finish("experience")
    public static void relateExperience(Employee employee, String experience){
        employee.setExperience(Integer.parseInt(experience));
    }
}
~~~

Let us walk through the code:

~~~java
import jlibs.xml.sax.binding.*;
~~~

this package contains various annotations, which we use to define binding.

~~~java
@Binding("employee")
public class EmployeeBinding{
~~~

`@Binding("employee")` annotation says that, `EmployeeBinding` class defines binding for `<employee>` element

~~~java
@Binding.Start
public static Employee onStart() throws SAXException{
    return new Employee();
}
~~~

`@Binding.Start` annotation says that, when `<employee>` element starts call this method.  
this method returns new `Employee` object. i.e for each `<employee>` we create one `Employee` object.

~~~java
@Binding.Text({"name", "age", "experience"})
public static String onText(String text){
    return text;
}
~~~

`@Binding.Text({"name", "age", "experience"})` annotation says that,  
call this method for `<name>`, `<age>` and `<employee>` text content.  
The argument `text` will be the text content of that element.  
The java object created for these elements is their text  
content, so we simply return the `text` argument.

~~~java
@Relation.Finish("name")
public static void relateName(Employee employee, String name){
    employee.setName(name);
}
~~~

`@Relation.Finish("name")` annotation says that, call this method on `</name>`.  
The first argument will be the java object created for `<name>`'s parent element (i,e `<employee>` element), which is `Employee` object created by `onStart()` method.  
The second argument will be the java object created for `<name>` element, which is `String` object created by `onText(...)` method.

similarly `relateAge(...)` and `relateExperience(...)` are called on `<age>` and `<experience>` element end respectively.

## SAX Parsing ##

Now we have finished coding `EmployeeBinding`. Now let us see how to read employee xml document using this binding.

~~~java
public static Employee read(File xmlFile) throws Exception{
    BindingHandler handler = new BindingHandler(EmployeeBinding.class);
    return (Employee)handler.parse(new InputSource(xmlFile.getPath()));
}
~~~

`BindingHandler` is an implementation of SAX `DefaultHandler`. It's constructor takes the binding calss as argument.

## Behind the Scene ##

Let us see what happens behind the scene.

All the annoations we have used have `RetentionPolicy.SOURCE` (except `@Binding`). i.e These annotations are not available at runtime.  
`JLibs` comes with an [annotation processor](http://code.google.com/p/jlibs/source/browse/trunk/xml/src/jlibs/xml/sax/binding/BindingAnnotationProcessor.java),
which processes these annotations at compile time.  
This processor generates a class for each class with `@Binding` annotation.  
This generated class defines the [state diagram](http://en.wikipedia.org/wiki/State_diagram) for the binding.

for example when you compile `EmployeeBinding` class, `EmployeeBindingImpl.java` is generated.

`BindingHandler` is a SAX `DefaultHandler` which implements a [state machine](http://en.wikipedia.org/wiki/Finite-state_machine).

Because reflection is not used at runtime, the sax parsing will be faster.

## Unexpected Elements ##

let us say employee.xml is:

~~~xml
<employee>                     
    <name>scott</name>         
    <age>20</age>              
    <experience>5</experience> 
    <email>scott@email.com</email> 
</employee>
~~~

The above xml document has an unexpected element `<email>` for which we have not defined any binding.

When you read the above xml document using `EmployeeBinding`, it simply ignores the undefinded element `<email>`.  
i.e you will be able to create `Employee` object from the above xml document without any errors.

Suppose you want to issue an error for undefined elements then do:

~~~java
handler.setIgnoreUnresolved(false); // default is true
~~~

now when you try to read the above xml document, you will get following exception:

~~~
org.xml.sax.SAXException: can't find binding for /employee/email (line=5, col=12)
~~~

## Reusing Bindings ##

Have a look at `Company` class:

~~~java
public class Company{
    private String name;
    private Employee manager;
    public List<Employee> employees = new ArrayList<Employee>();
    
    // getter and setter methods
}
~~~

and company.xml as below:

~~~
        XML                        |     Java Object                   |   Relation
-----------------------------------|-------------------------------------------------------------------
<company name="foo">               | company = new Company(@name)      |
    <manager>                      | manager = /*use EmployeeBinding*/ |
        <name>admin</name>         |                                   |
        <age>30</age>              |                                   |
        <experience>7</experience> |                                   |
    </manager>                     |                                   | company.setManager(manager)
    <employee>                     | employee = /*use EmployeeBinding*/|
        <name>scott</name>         |                                   |
        <age>20</age>              |                                   |
        <experience>5</experience> |                                   |
    </employee>                    |                                   | company.addEmployee(employee)
    <employee>                     | employee = /*use EmployeeBinding*/|
        <name>alice</name>         |                                   |
        <age>21</age>              |                                   |
        <experience>4</experience> |                                   |
    </employee>                    |                                   | company.addEmployee(employee)
</company>                         |                                   |
~~~

on `<company>` element begin, we create `Company` object.  
for `<manager>` and `<employee>` elements we will create `Employee` objects, using `EmployeeBinding` coded earlier.  
on `<manager>` element end, we relate `company` and `manager` objects using `setManager(...)` method  
on `<employee>` element end, we relate `company` and `employee` objects using `addEmployee(...)` method

Let us implment `CompanyBinding`:

~~~java
@Binding("company")
public class CompanyBinding{
    @Binding.Start
    public static Company onStart(@Attr String name) throws SAXException{
        return new Company(name);
    }

    @Binding.Element(element = "manager", clazz = EmployeeBinding.class)
    public static void onManager(){}

    @Relation.Finish("manager")
    public static void relateManager(Company company, Employee manager){
        company.setManager(manager);
    }

    @Binding.Element(element = "employee", clazz = EmployeeBinding.class)
    public static void onEmployee(){}

    @Relation.Finish("employee")
    public static void relateEmployee(Company company, Employee employee){
        company.employees.add(employee);
    }
}
~~~

Let us walk through the code:

~~~java
@Binding.Start
public static Company onStart(@Attr String name) throws SAXException{
    return new Company(name);
}
~~~

`@Attr` annotation is used to get attribute value.  
the attribute name is derived from the parameter name.  
i.e, `@Attr String name` will give value of attribute `name`

in some cases, attribute name may not be valid java identifier.  
for example `param-name` is a valid attribute name, but not a valid java identifier.  
in such cases you can do:

~~~java
@Attr("param-name") String paramName
~~~

~~~java
@Binding.Element(element = "manager", clazz = EmployeeBinding.class)
public static void onManager(){}
~~~

`@Binding.Element(element = "manager", clazz = EmployeeBinding.class)` annotation says to reuse `EmployeeBinding` for `<manager>` element.

## Namespace Support ##

~~~java
@NamespaceContext({
    @Entry(prefix="foo", uri="http://www.foo.com"),
    @Entry(prefix="bar", uri="http://www.bar.com")
})
@Binding("foo:employee")
public class EmployeeBinding{
    ...
}
~~~

`@NamespaceContext` annotation is used to define prefix to namespace mappings.  
then you can use these prefixes in remaining annotations. for example:  

~~~java
@Binding("foo:employee")
~~~

## When no-arg constructor is missing ##

Let us say our <code>Employee</code> has no default constructor:

~~~ java
public class Employee{
    private String name;
    private int age;
    private int experience;

    public Employee(String name, int age, int experience){
        this.name = name;
        this.age = age;
        this.experience = experience;
    }

    // getter methods
}
~~~

Let us see how to handle this situation:

~~~
        XML                     |     Java Object                                                     |   Relation
--------------------------------|---------------------------------------------------------------------|----------------------------
<employee>                      |                                                                     |
    <name>scott</name>          | name = new String(#text)                                            | parent[<name>] = name
    <age>20</age>               | age = new String(#text)                                             | parent[<age>] = age
    <experience>5</experience>  | exp = new String(#text)                                             | parent[<experience>] = exp
</employee>                     | emp = new Employee(current[<name>], current[<age>], [<experience>]) | 
~~~

Notice that now we are creating `Employee` object at `</employee>`, rather than `<employee>` element begin.  
This is because, the values of name, age and experience are available only on `<employee>` element end.

Earlier, the relation for `<name>`, `<age>` and `<experience>` used to call respective set method on `Employee` object.  
Now we can't do that, because we don't have `Employee` created by that time.

For each element in xml document, a [SAXContext](http://code.google.com/p/jlibs/source/browse/trunk/xml/src/jlibs/xml/sax/binding/SAXContext.java) is maintained by `BindingHandler`.  
This context is used to store the java object you created for that element.  
`SAXContext.object` gives the object you have created.

Other than that, `SAXContext` also has map, which you can use to store values temporarly which are required to create the java object later.  
`SAXContext.temp` is that map. here key is `QName` and value is any `Object`.

let us see the relation for `<name>` element:

~~~
parent[<name>] = name
~~~

here `parent` refers to parent element's (i,e `<employee>`) context;  
we are storing the `String` object created for `<name>` element, in `<employee>`'s context.  
here the key used is the qname name of current element i.e `<name>`

similarly we are saving the values of `<age>` and `<experience>` elements also in `<employees>`'s context.

now on `</employee>` element end we do:

~~~
emp = new Employee(current[<name>], current[<age>], [<experience>])
~~~

here `current` refers to current element's (i.e `<employee>`) context;  
we are retriving the values of `<name>`, `<age>` and `<experience>` stored earlier, and creating `Employee` object using them.

Let us see what it looks like in java code:

~~~java
@Binding("employee")
public class EmployeeBinding{
    @Binding.Text({"name", "age", "experience"})
    public static String onText(String text){
        return text;
    }

    @Relation.Finish({"name", "age", "experience"})
    public static String relateWithEmployee(String value){
        return value;
    }

    @Binding.Finish
    public static Employee onFinish(@Temp String name, @Temp String age, @Temp String experience) throws SAXException{
        return new Employee(name,
                Integer.parseInt(age==null ? "0" : age),
                Integer.parseInt(experience==null ? "0" : experience)
        );
    }
}
~~~

Let us walk through the code;

~~~
@Relation.Finish({"name", "age", "experience"})
public static String relateWithEmployee(String value){
    return value;
}
~~~

`@Relation.Finish({"name", "age", "experience"})` annotation says that, call this method on
`<name>`, `<age>` and `<experience>` element end.

When a method with relation annotation returns something:

- the returned value is stored in parent element's temp with current element's qname as key
- the first argument will be the object created for the current element

i.e <code>String value</code> is the <code>String</code> object created for current element.
and the value returned is stored in <code><employee></code> element's temp.

~~~java
@Binding.Finish
public static Employee onFinish(@Temp String name, @Temp String age, @Temp String experience) throws SAXException{
    return new Employee(name,
            Integer.parseInt(age==null ? "0" : age),
            Integer.parseInt(experience==null ? "0" : experience)
    );
}
~~~

`@Binding.Finish` annotation says that, call this method on `</employee>`.

`@Temp String name` says that give me the value mapped to `name` in current element's temp.  
similar to `@Attr` you can explicitly specify the qname as follows:  

~~~java
@Temp("name") String employeeName
~~~

Here in `onFinish(...)` method, we are creating `Employee` object using the values that are stored earlier in temp, and returning it.

## Recursive Bindings ##

Let us say we have `Component` class:

~~~java
public class Component{
    public final String name;
    public Properties initParams = new Properties();
    public List<Component> dependencies = new ArrayList<Component>();

    public Component(String name){
        this.name = name;
    }
}
~~~

each component has:

- name
- init params
- dependencies (list of components) i.e, we have recursion

Let us see sample xml:

~~~
        XML                                               |     Java Object                                                     |   Relation
----------------------------------------------------------|---------------------------------------------------------------------|----------------------------
<component name="comp1">                                  | comp = new Company(@name)                                           |
    <init-param>                                          |                                                                     |
        <param-name>param1</param-name>                   | paramName = #text                                                   | parent[<param-name>] = paramName
        <param-value>value1</param-value>                 | paramValue = #text                                                  | parent[<param-value>] = paramValue
    </init-param>                                         | comp.initParams.put(current[<param-name>], current[<param-value>])  |
    <init-param>                                          |                                                                     |
        <param-name>param2</param-name>                   |                                                                     |
        <param-value>value2</param-value>                 |                                                                     |
    </init-param>                                         |                                                                     |
    <dependencies>                                        |                                                                     |
        <component name="comp2">                          | dependent = /* use Recursion */                                     |
            <init-param>                                  |                                                                     |
                <param-name>param3</param-name>           |                                                                     |
                <param-value>value3</param-value>         |                                                                     |
            </init-param>                                 |                                                                     |
            <dependencies>                                |                                                                     |
                <component name="comp3">                  |                                                                     |
                    <init-param>                          |                                                                     |
                        <param-name>param4</param-name>   |                                                                     |
                        <param-value>value4</param-value> |                                                                     |
                    </init-param>                         |                                                                     |
                </component>                              |                                                                     |
            </dependencies>                               |                                                                     |
        </component>                                      |                                                                     | comp.dependencies.add(dependent)
    </dependencies>                                       |                                                                     |
</component>                                              |                                                                     |
~~~

here we have recursion. `comp1` depends on `comp2` which in turn depends on `comp3`

let us see the Binding implementation:

~~~java
@Binding("component")
public class ComponentBinding{
    @Binding.Start
    public static Component onStart(@Attr String name){
        return new Component(name);
    }

    @Binding.Text({ "init-param/param-name", "init-param/param-value" })
    public static String onParamNameAndValue(String text){
        return text;
    }

    @Relation.Finish({"init-param/param-name", "init-param/param-value"})
    public static String relateParamNameAndValue(String content){
        return content;
    }

    @Relation.Finish("init-param")
    public static void relateParam(Component comp, @Temp("param-name") String paramName, @Temp("param-value") String paramValue){
        comp.initParams.put(paramName, paramValue);
    }

    @Binding.Element(element="dependencies/component", clazz=ComponentBinding.class)
    public static void onDependecy(){}

    @Relation.Finish("dependencies/component")
    public static void relateDependency(Component comp, Component dependent){
        comp.dependencies.add(dependent);
    }
}
~~~

## Reading List of values ##


~~~java
public class Employee{
    public String name;
    public int age;
    public int experience;
    public String contacts[];

    public Employee(String name, int age, int experience){
        this.name = name;
        this.age = age;
        this.experience = experience;
    }
}
~~~

~~~
        XML                                     |     Java Object                              |   Relation
------------------------------------------------|----------------------------------------------|----------------------------
<employee name="scott" age="20" experience="5"> | emp = new Employee(@name, @age, @experience) |
    <contacts>                                  |                                              |
        <email>scott@yahoo.com</email>          | email = #text                                | parent[<email>] += email
        <email>scott@google.com</email>         | email = #text                                | parent[<email>] += email
        <email>scott@msn.com</email>            | email = #text                                | parent[<email>] += email
    </contacts>                                 |                                              | emp.contacts = current[<email>]
</employee>                                     |                                              |
~~~

notice that the relation for `<email>` element end is:

~~~
parent[<email>] += email
~~~

here `+=` means add to temp (i.e don't replace existing value)  
that is, `parent[<email>]` value is a list of strings rather than string

the relation for `<contacts>` element end is:

~~~
emp.contacts = parent[<email>]
~~~

i.e we are assigning the list of emails from current element's temp int `emp.conctacts`

Let us see the java code:

~~~java
@Binding("employee")
public class EmployeeBinding{
    @Binding.Start
    public static Employee onStart(@Attr String name, @Attr String age, @Attr String experience){
        return new Employee(name,
                age!=null ? Integer.parseInt(age) : 0,
                experience!=null ? Integer.parseInt(experience) : 0
        );
    }

    @Binding.Text("contacts/email")
    public static String onText(String text){
        return text;
    }

    @Relation.Finish("contacts/email")
    public static @Temp.Add String relateEmail(String email){
        return email;
    }

    @Binding.Finish("contacts")
    public static void onFinish(Employee emp, @Temp("email") List<String> emails){
        emp.contacts = emails.toArray(new String[emails.size()]);
    }
}
~~~

let us walk through the code:

~~~java
@Relation.Finish("contacts/email")
public static @Temp.Add String relateEmail(String email){
    return email;
}
~~~

`@Temp.Add` on return type says that, add the returned value to the existing value.  
i.e we want to save it as list of emails

~~~java
@Binding.Finish("contacts")
public static void onFinish(Employee emp, @Temp("email") List<String> emails){
    if(emails!=null)
        emp.contacts = emails.toArray(new String[emails.size()]);
}
~~~

notice the second argument. It is mapped to `@Temp("email")` and its type is `List<String>`

## Comparision with existing Binding Frameworks ##

Most of java-xml binding implementations provide two way support.

- serializing domain object to xml
- serializing xml to domain object

But JLibs implementation is only one way. that is *deserializing xml to domain object*.  
to serialize xml to domain object, you can use `XMLDocument`

the main advantages of JLibs implementation:

- works with hand-coded domain objects

most binding implementations mandate that domain objects has to be generated  
from schema or dtd. They don't work with hand-coded domain objects

- domain objects are not tied to binding implementation

domain objects dont need to extend/implement a particular class/interface from binding implementation.
for example:

- XMLBeans mandates that all domain objects implement `org.apache.xmlbeans.XmlObject`
- Similarly EMF mandates that all domain objects implement `EObject` interface

- domain objects are light-weight. i.e not poluted with binding implementation specific information

for example: the domain objects created by XMLBeans or EMF carry lot of information which are specific
to them. This will bloat up memory.

- easier migration

let us say in version 2 of your project, you want to change the structure of xml how it looks like, and still wants to provide backward
compatibility to end-users.

This is tedious task with other binding frameworks. With JLibs you can have different Binding implementations
for a domain object and use appropriate one at runtime based on version of xml document.

- clear separation of binding and domain object

in jlibs, it is like a callback methods. callback methods can define when it has to be called and what information from xml document you want. 
You have complete control how to consume that information into domain object (because you are implementing it in java code)

- The runtime memory used by jlibs binding is minimal and no reflection api is used. The number of `SAXContext` is equal to the maximum element depth of the xml document.

JLibs binding implementation more of resembels [Apache's Commons-Digestor](http://commons.apache.org/digester/) but without reflection


