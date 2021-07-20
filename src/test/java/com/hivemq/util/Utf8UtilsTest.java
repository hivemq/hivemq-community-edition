/*
 * Copyright 2019-present HiveMQ GmbH
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.hivemq.util;

import com.google.common.base.Charsets;
import com.google.common.base.Utf8;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * @author Lukas Brandl
 */
public class Utf8UtilsTest {

    // 1 Byte (94 characters) :
    // !"#$%&'()*+,-./0123456789:;<=>?@ABCDEFGHIJKLMNOPQRSTUVWXYZ[\]^_`abcdefghijklmnopqrstuvwxyz{|}~

    // 2 Byte (94 characters):
    // ¡¢£¤¥¦§¨©ª«¬®¯°±²³´µ¶·¸¹º»¼½¾¿ÀÁÂÃÄÅÆÇÈÉÊËÌÍÎÏÐÑÒÓÔÕÖ×ØÙÚÛÜÝÞßàáâãäåæçèéêëìíîïðñòóôõö÷øùúûüýþÿ

    // 3 Byte (130 characters):
    // ঁংঃ঄অআইঈউঊঋঌ঍঎এঐ঑঒ওঔকখগঘঙচছজঝঞটঠডঢণতথদধন঩পফবভমযর঱ল঳঴঵শষসহ঺঻়ঽািীুূৃৄ৅৆েৈ৉৊োৌ্ৎ৏৐৑৒৓৔৕৖ৗ৘৙৚৛ড়ঢ়৞য়ৠৡৢৣ৤৥০১২৩৪৫৬৭৮৯ৰৱ৲৳৴৵৶৷৸৹৺৻ৼ৽৾৿

    // 4 Byte (128)
    // 𐨀𐨁𐨂𐨃𐨄𐨅𐨆𐨇𐨈𐨉𐨊𐨋𐨌𐨍𐨎𐨏𐨐𐨑𐨒𐨓𐨔𐨕𐨖𐨗𐨘𐨙𐨚𐨛𐨜𐨝𐨞𐨟𐨠𐨡𐨢𐨣𐨤𐨥𐨦𐨧𐨨𐨩𐨪𐨫𐨬𐨭𐨮𐨯𐨰𐨱𐨲𐨳𐨴𐨵𐨶𐨷𐨹𐨺𐨸𐨻𐨼𐨽𐨾𐨿𐩀𐩁𐩂𐩃𐩄𐩅𐩆𐩇𐩈𐩉𐩊𐩋𐩌𐩍𐩎𐩏𐩐𐩑𐩒𐩓𐩔𐩕𐩖𐩗𐩘𐩙𐩚𐩛𐩜𐩝𐩞𐩟𐩠𐩡𐩢𐩣𐩤𐩥𐩦𐩧𐩨𐩩𐩪𐩫𐩬𐩭𐩮𐩯𐩰𐩱𐩲𐩳𐩴𐩵𐩶𐩷𐩸𐩹𐩺𐩻𐩼𐩽𐩾𐩿

    @Test
    public void test_string_is_one_byte_char_only() {
        assertTrue(Utf8Utils.stringIsOneByteCharsOnly("!\"#$%&'()*+,-./0123456789:;<=>?@ABCDEFGHIJKLMNOPQRSTUVWXYZ[\\]^_`abcdefghijklmnopqrstuvwxyz{|}~"));
        assertFalse(Utf8Utils.stringIsOneByteCharsOnly("abc¡123"));
        assertFalse(Utf8Utils.stringIsOneByteCharsOnly("abcআ123"));
        assertFalse(Utf8Utils.stringIsOneByteCharsOnly("abc\uD802\uDE6F123"));
    }

    @Test
    public void test_string_utf_8_length() {

        assertEquals(94, Utf8Utils.encodedLength("!\"#$%&'()*+,-./0123456789:;<=>?@ABCDEFGHIJKLMNOPQRSTUVWXYZ[\\]^_`abcdefghijklmnopqrstuvwxyz{|}~"));
        assertEquals(94 * 2, Utf8Utils.encodedLength("¡¢£¤¥¦§¨©ª«¬®¯°±²³´µ¶·¸¹º»¼½¾¿ÀÁÂÃÄÅÆÇÈÉÊËÌÍÎÏÐÑÒÓÔÕÖ×ØÙÚÛÜÝÞßàáâãäåæçèéêëìíîïðñòóôõö÷øùúûüýþÿ"));
        assertEquals(130 * 3, Utf8Utils.encodedLength("ঁংঃ\u0984অআইঈউঊঋঌ\u098D\u098Eএঐ\u0991\u0992ওঔকখগঘঙচছজঝঞটঠডঢণতথদধন\u09A9পফবভমযর\u09B1ল\u09B3\u09B4\u09B5শষসহ\u09BA\u09BB়ঽািীুূৃৄ\u09C5\u09C6েৈ\u09C9\u09CAোৌ্ৎ\u09CF\u09D0\u09D1\u09D2\u09D3\u09D4\u09D5\u09D6ৗ\u09D8\u09D9\u09DA\u09DBড়ঢ়\u09DEয়ৠৡৢৣ\u09E4\u09E5০১২৩৪৫৬৭৮৯ৰৱ৲৳৴৵৶৷৸৹৺৻\u09FC\u09FD\u09FE\u09FF"));
        assertEquals(128 * 4, Utf8Utils.encodedLength("\uD802\uDE00\uD802\uDE01\uD802\uDE02\uD802\uDE03\uD802\uDE04\uD802\uDE05\uD802\uDE06\uD802\uDE07\uD802\uDE08\uD802\uDE09\uD802\uDE0A\uD802\uDE0B\uD802\uDE0C\uD802\uDE0D\uD802\uDE0E\uD802\uDE0F\uD802\uDE10\uD802\uDE11\uD802\uDE12\uD802\uDE13\uD802\uDE14\uD802\uDE15\uD802\uDE16\uD802\uDE17\uD802\uDE18\uD802\uDE19\uD802\uDE1A\uD802\uDE1B\uD802\uDE1C\uD802\uDE1D\uD802\uDE1E\uD802\uDE1F\uD802\uDE20\uD802\uDE21\uD802\uDE22\uD802\uDE23\uD802\uDE24\uD802\uDE25\uD802\uDE26\uD802\uDE27\uD802\uDE28\uD802\uDE29\uD802\uDE2A\uD802\uDE2B\uD802\uDE2C\uD802\uDE2D\uD802\uDE2E\uD802\uDE2F\uD802\uDE30\uD802\uDE31\uD802\uDE32\uD802\uDE33\uD802\uDE34\uD802\uDE35\uD802\uDE36\uD802\uDE37\uD802\uDE39\uD802\uDE3A\uD802\uDE38\uD802\uDE3B\uD802\uDE3C\uD802\uDE3D\uD802\uDE3E\uD802\uDE3F\uD802\uDE40\uD802\uDE41\uD802\uDE42\uD802\uDE43\uD802\uDE44\uD802\uDE45\uD802\uDE46\uD802\uDE47\uD802\uDE48\uD802\uDE49\uD802\uDE4A\uD802\uDE4B\uD802\uDE4C\uD802\uDE4D\uD802\uDE4E\uD802\uDE4F\uD802\uDE50\uD802\uDE51\uD802\uDE52\uD802\uDE53\uD802\uDE54\uD802\uDE55\uD802\uDE56\uD802\uDE57\uD802\uDE58\uD802\uDE59\uD802\uDE5A\uD802\uDE5B\uD802\uDE5C\uD802\uDE5D\uD802\uDE5E\uD802\uDE5F\uD802\uDE60\uD802\uDE61\uD802\uDE62\uD802\uDE63\uD802\uDE64\uD802\uDE65\uD802\uDE66\uD802\uDE67\uD802\uDE68\uD802\uDE69\uD802\uDE6A\uD802\uDE6B\uD802\uDE6C\uD802\uDE6D\uD802\uDE6E\uD802\uDE6F\uD802\uDE70\uD802\uDE71\uD802\uDE72\uD802\uDE73\uD802\uDE74\uD802\uDE75\uD802\uDE76\uD802\uDE77\uD802\uDE78\uD802\uDE79\uD802\uDE7A\uD802\uDE7B\uD802\uDE7C\uD802\uDE7D\uD802\uDE7E\uD802\uDE7F"));

    }

    @Test
    public void test_is_well_formed_utf_8() {

        for (int b = 0xA0; b <= 0xBF; b++) {
            for (int b2 = 0; b2 < 0xFF; b2++) {

                final byte[] bytes = new byte[]{'a', 'b', 'c', (byte) 0xED, (byte) b, (byte) b2, 'd', 'e', 'f'};

                final ByteBuf buf = Unpooled.buffer();

                buf.writeShort(bytes.length);
                buf.writeBytes(bytes);

                //checking both original guava method for byte-arrays and the ByteBuf implementation
                assertFalse(Utf8Utils.isWellFormed(buf, bytes.length));
                assertFalse(Utf8.isWellFormed(bytes));

                buf.release();
            }
        }
    }

    @Test
    public void test_should_not_control_characters() {
        for (char c = '\u0001'; c <= '\u001F'; c++) {
            final byte[] bytes = {'a', 'b', 'c', (byte) c, 'd', 'e', 'f'};
            assertTrue(Utf8Utils.hasControlOrNonCharacter(bytes));
        }

        {
            final byte[] bytes = {'a', 'b', 'c', 0x7F, 'd', 'e', 'f'};
            assertTrue(Utf8Utils.hasControlOrNonCharacter(bytes));
        }
        for (int b = 0x80; b <= 0x9F; b++) {
            final byte[] bytes = {'a', 'b', 'c', (byte) 0xC2, (byte) b, 'd', 'e', 'f'};
            assertTrue(Utf8Utils.hasControlOrNonCharacter(bytes));
        }

        final byte[] bytes = {'a', 'b', 'c', (byte) 0xEF, (byte) 0x7F};
        assertTrue(Utf8Utils.hasControlOrNonCharacter(bytes));
    }

    @Test
    public void test_should_not_non_characters() {
        for (int c = 0xFFFE; c <= 0x10_FFFE; c += 0x1_0000) {
            for (int i = 0; i < 2; i++) {
                final String nonCharacterString = String.valueOf(Character.toChars(c + i));
                final String testString = "abc" + nonCharacterString + "def";
                assertTrue(Utf8Utils.hasControlOrNonCharacter(testString));
            }
        }
    }

    @Test
    public void test_should_not_non_characters_binary() {
        for (int c = 0xFFFE; c <= 0x10_FFFE; c += 0x1_0000) {
            for (int i = 0; i < 2; i++) {
                final String nonCharacterString = String.valueOf(Character.toChars(c + i));
                final byte[] nonCharacterBinary = nonCharacterString.getBytes(Charsets.UTF_8);
                final byte[] binary = new byte[6 + nonCharacterBinary.length];
                binary[0] = 'a';
                binary[1] = 'b';
                binary[2] = 'c';
                System.arraycopy(nonCharacterBinary, 0, binary, 3, nonCharacterBinary.length);
                binary[3 + nonCharacterBinary.length] = 'd';
                binary[3 + nonCharacterBinary.length + 1] = 'e';
                binary[3 + nonCharacterBinary.length + 2] = 'f';
                assertTrue(Utf8Utils.hasControlOrNonCharacter(binary));
            }
        }
    }
}