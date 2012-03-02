copalis.sql is a toolkit designed to make mapping from SQL to Java as easy as possible.
The objective is not to shield the programmer from SQL,
but to add type safety and simplicity to JDBC.

copalis.sql was inspired by [TMDBC](https://tmdbc.dev.java.net/), but departs from it in these respects:

+ Uses standard JDBC and is not tied to Postgres
+ Uses runtime dynamic proxies rather than compile-time code generation
+ Data access interfaces are wrappers around `ResultSet`, not Java Beans

### How it Works

The idea is that you define an interface containing database access methods,
and annotate them with the SQL commands that should be executed when they are invoked.
copalis.sql will generate an implementation of the interface that performs all the necessary
parameter and result set wrapping.

### A Simple Query

    interface AddressSession extends Session {
        @Query("select ZIP from ADDRESSES where NAME=$1") String zipCode(String name);
    }

Here the `@Query` annotation specifies SQL that should be executed when `zipCode` is invoked.
The value of `name` will be bound to the parameter `$1`,
and if one or more records are selected,
the value of `ZIP` from the first selected record is returned,
otherwise `null` is returned.

An instance of `AddressSession` is created and used as follows:

    AddressSession session = Connecting.to("jdbc://my-database").as(AddressSession.class).open();
    System.out.println("Smith's zip code is: " + session.zipCode("Smith"));
    session.close();

Once the `AddressSession` is no longer needed, it is closed using the `close` method inherited from `Session`.

### A Less Simple Query

Most of the time when we perform a SQL SELECT,
we may get more than one record,
and want to read multiple fields.
To do this in copalis.sql,
define an interface extending `Results` for accessing the field values:

    interface Addresses extends Results {
        String name();
        String street();
        String city();
        String state();
        String zip();
    }

Now we can add another method declaration to `AddressSession`:

        @Query("select * from ADDRESSES where NAME=$1") Addresses forName(String name);

The new method returns an instance of `Addresses`,
which can be iterated over as follows:

        Addresses addrs = session.forName("Jones");
        while (addrs.next()) {
            System.out.println(addrs.name() + " lives in city: " + addrs.city());
        }
        addrs.close();

The `next()` and `close()` methods are inherited from the `Results` interface.
