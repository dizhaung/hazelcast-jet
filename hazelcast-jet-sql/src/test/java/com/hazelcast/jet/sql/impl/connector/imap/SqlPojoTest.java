/*
 * Copyright (c) 2008-2020, Hazelcast, Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.hazelcast.jet.sql.impl.connector.imap;

import com.hazelcast.jet.sql.SqlTestSupport;
import com.hazelcast.jet.sql.impl.connector.imap.model.AllTypesValue;
import com.hazelcast.jet.sql.impl.connector.imap.model.Person;
import com.hazelcast.jet.sql.impl.connector.imap.model.PersonId;
import com.hazelcast.sql.impl.connector.LocalPartitionedMapConnector;
import org.junit.Ignore;
import org.junit.Test;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Date;
import java.util.GregorianCalendar;

import static com.hazelcast.jet.core.TestUtil.createMap;
import static com.hazelcast.sql.impl.connector.SqlConnector.JAVA_SERIALIZATION_FORMAT;
import static com.hazelcast.sql.impl.connector.SqlKeyValueConnector.TO_KEY_CLASS;
import static com.hazelcast.sql.impl.connector.SqlKeyValueConnector.TO_SERIALIZATION_KEY_FORMAT;
import static com.hazelcast.sql.impl.connector.SqlKeyValueConnector.TO_SERIALIZATION_VALUE_FORMAT;
import static com.hazelcast.sql.impl.connector.SqlKeyValueConnector.TO_VALUE_CLASS;
import static java.lang.String.format;
import static java.time.Instant.ofEpochMilli;
import static java.time.ZoneId.systemDefault;
import static java.time.ZoneOffset.UTC;
import static java.util.Collections.singletonList;

// TODO: move it to IMDG when INSERTs are supported, or at least move to one of Jet connector tests ?
public class SqlPojoTest extends SqlTestSupport {

    @Test
    public void supportsNulls() {
        String name = createMapWithRandomName();

        assertMapEventually(
                name,
                format("INSERT OVERWRITE %s VALUES (null, null)", name),
                createMap(new PersonId(), new Person()));

        assertRowsEventuallyAnyOrder(
                format("SELECT * FROM %s", name),
                singletonList(new Row(0, null)));
    }

    @Test
    public void supportsFieldsShadowing() {
        String name = createMapWithRandomName();

        assertMapEventually(
                name,
                format("INSERT OVERWRITE %s (birthday, id) VALUES ('2020-01-01', 1)", name),
                createMap(new PersonId(1), new Person(0, LocalDate.of(2020, 1, 1))));

        assertRowsEventuallyAnyOrder(
                format("SELECT id, birthday FROM %s", name),
                singletonList(new Row(1, LocalDate.of(2020, 1, 1))));
    }

    @Test
    public void supportsFieldsRemapping() {
        String name = generateRandomName();
        executeSql(format("CREATE EXTERNAL TABLE %s (" +
                        " key_id INT EXTERNAL NAME \"__key.id\"," +
                        " value_id INT EXTERNAL NAME \"this.id\"" +
                        ")" +
                        "TYPE \"%s\" " +
                        "OPTIONS (" +
                        " \"%s\" '%s'," +
                        " \"%s\" '%s'," +
                        " \"%s\" '%s'," +
                        " \"%s\" '%s'" +
                        ")",
                name, LocalPartitionedMapConnector.TYPE_NAME,
                TO_SERIALIZATION_KEY_FORMAT, JAVA_SERIALIZATION_FORMAT,
                TO_KEY_CLASS, PersonId.class.getName(),
                TO_SERIALIZATION_VALUE_FORMAT, JAVA_SERIALIZATION_FORMAT,
                TO_VALUE_CLASS, Person.class.getName()
        ));

        assertMapEventually(
                name,
                format("INSERT OVERWRITE %s (key_id, value_id, birthday) VALUES (1, 2, '2020-01-01')", name),
                createMap(new PersonId(1), new Person(2, LocalDate.of(2020, 1, 1))));

        assertRowsEventuallyAnyOrder(
                format("SELECT key_id, value_id, birthday FROM %s", name),
                singletonList(new Row(1, 2, LocalDate.of(2020, 1, 1))));
    }

    @Test
    @Ignore // TODO: no accessor for ssn
    public void supportsFieldsExtensions() {
        String name = generateRandomName();
        executeSql(format("CREATE EXTERNAL TABLE %s (" +
                        " ssn VARCHAR" +
                        ") TYPE \"%s\" " +
                        "OPTIONS (" +
                        " \"%s\" '%s'," +
                        " \"%s\" '%s'," +
                        " \"%s\" '%s'," +
                        " \"%s\" '%s'" +
                        ")",
                name, LocalPartitionedMapConnector.TYPE_NAME,
                TO_SERIALIZATION_KEY_FORMAT, JAVA_SERIALIZATION_FORMAT,
                TO_KEY_CLASS, PersonId.class.getName(),
                TO_SERIALIZATION_VALUE_FORMAT, JAVA_SERIALIZATION_FORMAT,
                TO_VALUE_CLASS, Person.class.getName()
        ));

        assertMapEventually(
                name,
                format("INSERT OVERWRITE %s (id, birthday, ssn) VALUES (1, '2020-01-01', null)", name),
                createMap(new PersonId(1), new Person(0, LocalDate.of(2020, 1, 1))));

        assertRowsEventuallyAnyOrder(
                format("SELECT id, birthday, ssn FROM %s", name),
                singletonList(new Row(1, LocalDate.of(2020, 1, 1), null)));
    }

    @Test
    public void supportsAllTypes() {
        String name = generateRandomName();
        executeSql(format("CREATE EXTERNAL TABLE %s " +
                        "TYPE \"%s\" " +
                        "OPTIONS (" +
                        " \"%s\" '%s'," +
                        " \"%s\" '%s'," +
                        " \"%s\" '%s'," +
                        " \"%s\" '%s'" +
                        ")",
                name, LocalPartitionedMapConnector.TYPE_NAME,
                TO_SERIALIZATION_KEY_FORMAT, JAVA_SERIALIZATION_FORMAT,
                TO_KEY_CLASS, BigInteger.class.getName(),
                TO_SERIALIZATION_VALUE_FORMAT, JAVA_SERIALIZATION_FORMAT,
                TO_VALUE_CLASS, AllTypesValue.class.getName()
        ));

        assertMapEventually(
                name,
                format("INSERT OVERWRITE %s (" +
                                " __key," +
                                " string," +
                                " character0," +
                                " character1," +
                                " boolean0," +
                                " boolean1," +
                                " byte0," +
                                " byte1," +
                                " short0," +
                                " short1," +
                                " int0, " +
                                " int1," +
                                " long0," +
                                " long1," +
                                " bigDecimal," +
                                " bigInteger," +
                                " float0," +
                                " float1," +
                                " double0," +
                                " double1," +
                                " \"localTime\"," +
                                " localDate," +
                                " localDateTime," +
                                " \"date\"," +
                                " calendar," +
                                " instant," +
                                " zonedDateTime," +
                                " offsetDateTime" +
                                ") VALUES (\n" +
                                "1, --key\n" +
                                "'string', --varchar\n" +
                                "'a', --character\n" +
                                "'b',\n" +
                                "true, --boolean\n" +
                                "false,\n" +
                                "126, --byte\n" +
                                "127, \n" +
                                "32766, --short\n" +
                                "32767, \n" +
                                "2147483646, --int \n" +
                                "2147483647,\n" +
                                "9223372036854775806, --long\n" +
                                "9223372036854775807,\n" +
                                "9223372036854775.123, --bigDecimal\n" +
                                "9223372036854775222, --bigInteger\n" +
                                "1234567890.1, --float\n" +
                                "1234567890.2, \n" +
                                "123451234567890.1, --double\n" +
                                "123451234567890.2,\n" +
                                "time'12:23:34', -- local time\n" +
                                "date'2020-04-15', -- local date \n" +
                                "timestamp'2020-04-15 12:23:34.1', --timestamp\n" +
                                "timestamp'2020-04-15 12:23:34.2', --timestamp with tz\n" +
                                "timestamp'2020-04-15 12:23:34.3', --timestamp with tz\n" +
                                "timestamp'2020-04-15 12:23:34.4', --timestamp with tz\n" +
                                "timestamp'2020-04-15 12:23:34.5', --timestamp with tz\n" +
                                "timestamp'2020-04-15 12:23:34.6' --timestamp with tz\n" +
                                ")",
                        name
                ),
                createMap(BigInteger.valueOf(1), new AllTypesValue(
                        "string",
                        'a',
                        'b',
                        true,
                        false,
                        (byte) 126,
                        (byte) 127,
                        (short) 32766,
                        (short) 32767,
                        2147483646,
                        2147483647,
                        9223372036854775806L,
                        9223372036854775807L,
                        new BigDecimal("9223372036854775.123"),
                        new BigInteger("9223372036854775222"),
                        1234567890.1f,
                        1234567890.2f,
                        123451234567890.1,
                        123451234567890.2,
                        LocalTime.of(12, 23, 34),
                        LocalDate.of(2020, 4, 15),
                        // TODO: should be LocalDateTime.of(2020, 4, 15, 12, 23, 34, 100_000_000)
                        //  when temporal types are fixed
                        LocalDateTime.of(2020, 4, 15, 12, 23, 34, 0),
                        Date.from(ofEpochMilli(1586953414200L)),
                        GregorianCalendar.from(ZonedDateTime.of(2020, 4, 15, 12, 23, 34, 300_000_000, UTC)
                                                            .withZoneSameInstant(localOffset())),
                        ofEpochMilli(1586953414400L),
                        ZonedDateTime.of(2020, 4, 15, 12, 23, 34, 500_000_000, UTC)
                                     .withZoneSameInstant(localOffset()),
                        ZonedDateTime.of(2020, 4, 15, 12, 23, 34, 600_000_000, UTC)
                                     .withZoneSameInstant(systemDefault())
                                     .toOffsetDateTime()
                )));

        assertRowsEventuallyAnyOrder(
                format("SELECT" +
                                " __key," +
                                " string," +
                                " character0," +
                                " character1," +
                                " boolean0," +
                                " boolean1," +
                                " byte0," +
                                " byte1," +
                                " short0," +
                                " short1," +
                                " int0," +
                                " int1," +
                                " long0," +
                                " long1," +
                                " bigDecimal," +
                                " bigInteger," +
                                " float0," +
                                " float1," +
                                " double0," +
                                " double1," +
                                " \"localTime\"," +
                                " localDate," +
                                " localDateTime," +
                                " \"date\"," +
                                " calendar," +
                                " instant," +
                                " zonedDateTime," +
                                " offsetDateTime" +
                                " FROM %s",
                        name
                ),
                singletonList(new Row(
                        BigDecimal.valueOf(1),
                        "string",
                        "a",
                        "b",
                        true,
                        false,
                        (byte) 126,
                        (byte) 127,
                        (short) 32766,
                        (short) 32767,
                        2147483646,
                        2147483647,
                        9223372036854775806L,
                        9223372036854775807L,
                        new BigDecimal("9223372036854775.123"),
                        new BigDecimal("9223372036854775222"),
                        1234567890.1f,
                        1234567890.2f,
                        123451234567890.1,
                        123451234567890.2,
                        LocalTime.of(12, 23, 34),
                        LocalDate.of(2020, 4, 15),
                        // TODO: should be LocalDateTime.of(2020, 4, 15, 12, 23, 34, 100_000_000)
                        //  when temporal types are fixed
                        LocalDateTime.of(2020, 4, 15, 12, 23, 34, 0),
                        OffsetDateTime.ofInstant(Date.from(ofEpochMilli(1586953414200L)).toInstant(), systemDefault()),
                        ZonedDateTime.of(2020, 4, 15, 12, 23, 34, 300_000_000, UTC)
                                     .withZoneSameInstant(localOffset())
                                     .toOffsetDateTime(),
                        OffsetDateTime.ofInstant(ofEpochMilli(1586953414400L), systemDefault()),
                        ZonedDateTime.of(2020, 4, 15, 12, 23, 34, 500_000_000, UTC)
                                     .withZoneSameInstant(localOffset())
                                     .toOffsetDateTime(),
                        ZonedDateTime.of(2020, 4, 15, 12, 23, 34, 600_000_000, UTC)
                                     .withZoneSameInstant(systemDefault())
                                     .toOffsetDateTime()
                )));
    }

    private static ZoneOffset localOffset() {
        return systemDefault().getRules().getOffset(LocalDateTime.now());
    }

    private static String createMapWithRandomName() {
        String name = generateRandomName();
        executeSql(format("CREATE EXTERNAL TABLE %s " +
                        "TYPE \"%s\" " +
                        "OPTIONS (" +
                        " \"%s\" '%s'," +
                        " \"%s\" '%s'," +
                        " \"%s\" '%s'," +
                        " \"%s\" '%s'" +
                        ")",
                name, LocalPartitionedMapConnector.TYPE_NAME,
                TO_SERIALIZATION_KEY_FORMAT, JAVA_SERIALIZATION_FORMAT,
                TO_KEY_CLASS, PersonId.class.getName(),
                TO_SERIALIZATION_VALUE_FORMAT, JAVA_SERIALIZATION_FORMAT,
                TO_VALUE_CLASS, Person.class.getName()
        ));
        return name;
    }

    private static String generateRandomName() {
        return "pojo_" + randomString().replace('-', '_');
    }
}