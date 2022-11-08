// Copyright (c) 2022 Erwin Kok. BSD-3-Clause license. See LICENSE file for more details.
package org.erwinkok.multiformat.multiaddress

import org.erwinkok.multiformat.cid.CidV1
import org.erwinkok.multiformat.multicodec.Multicodec
import org.erwinkok.multiformat.multihash.Multihash
import org.erwinkok.result.assertErrorResult
import org.erwinkok.result.expectNoErrors
import org.erwinkok.util.Hex
import org.erwinkok.util.Tuple2
import org.junit.jupiter.api.Assertions.assertArrayEquals
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.DynamicTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestFactory
import java.util.stream.Stream

internal class MultiaddressTest {
    @TestFactory
    fun `construction fails`(): Stream<DynamicTest> {
        return listOf(
            Tuple2("/ip4", "failed to parse Multiaddress /ip4: unexpected end of Multiaddress"),
            Tuple2("/ip4/::1", "failed to parse Multiaddress /ip4/::1: Invalid IPv4 address: ::1"),
            Tuple2("/ip4/fdpsofodsajfdoisa", "failed to parse Multiaddress /ip4/fdpsofodsajfdoisa: Invalid IPv4 address: fdpsofodsajfdoisa"),
            Tuple2("/ip4/127.0.0.1/ipcidr/256", "failed to parse Multiaddress /ip4/127.0.0.1/ipcidr/256: Invalid cpidr, must be <256"),
            Tuple2("/ip6/::/ipcidr/1026", "failed to parse Multiaddress /ip6/::/ipcidr/1026: Invalid cpidr, must be <256"),
            Tuple2("/ip6", "failed to parse Multiaddress /ip6: unexpected end of Multiaddress"),
            Tuple2("/ip6zone", "failed to parse Multiaddress /ip6zone: unexpected end of Multiaddress"),
            Tuple2("/ip6zone/", "failed to parse Multiaddress /ip6zone/: unexpected end of Multiaddress"),
            Tuple2("/ip6zone//ip6/fe80::1", "failed to parse Multiaddress /ip6zone//ip6/fe80::1: Empty IPv6Zone"),
            Tuple2("/udp", "failed to parse Multiaddress /udp: unexpected end of Multiaddress"),
            Tuple2("/tcp", "failed to parse Multiaddress /tcp: unexpected end of Multiaddress"),
            Tuple2("/sctp", "failed to parse Multiaddress /sctp: unexpected end of Multiaddress"),
            Tuple2("/udp/65536", "failed to parse Multiaddress /udp/65536: Failed to parse address 65536 (> 65535)"),
            Tuple2("/tcp/65536", "failed to parse Multiaddress /tcp/65536: Failed to parse address 65536 (> 65535)"),
            Tuple2("/quic/65536", "failed to parse Multiaddress /quic/65536: no protocol with name 65536"),
            Tuple2("/onion/9imaq4ygg2iegci7:80", "failed to parse Multiaddress /onion/9imaq4ygg2iegci7:80: Invalid onion address host: 9imaq4ygg2iegci7"),
            Tuple2("/onion/aaimaq4ygg2iegci7:80", "failed to parse Multiaddress /onion/aaimaq4ygg2iegci7:80: failed to parse onion address: aaimaq4ygg2iegci7:80 not a Tor onion address."),
            Tuple2("/onion/timaq4ygg2iegci7:0", "failed to parse Multiaddress /onion/timaq4ygg2iegci7:0: Port number is not in range(1, 65536): 0"),
            Tuple2("/onion/timaq4ygg2iegci7:-1", "failed to parse Multiaddress /onion/timaq4ygg2iegci7:-1: Port number is not in range(1, 65536): -1"),
            Tuple2("/onion/timaq4ygg2iegci7", "failed to parse Multiaddress /onion/timaq4ygg2iegci7: failed to parse onion address: timaq4ygg2iegci7 does not contain a port number"),
            Tuple2("/onion/timaq4ygg2iegci@:567", "failed to parse Multiaddress /onion/timaq4ygg2iegci@:567: Invalid onion address host: timaq4ygg2iegci@"),
            Tuple2("/onion3/9ww6ybal4bd7szmgncyruucpgfkqahzddi37ktceo3ah7ngmcopnpyyd:80", "failed to parse Multiaddress /onion3/9ww6ybal4bd7szmgncyruucpgfkqahzddi37ktceo3ah7ngmcopnpyyd:80: Invalid onion address host: 9ww6ybal4bd7szmgncyruucpgfkqahzddi37ktceo3ah7ngmcopnpyyd"),
            Tuple2("/onion3/vww6ybal4bd7szmgncyruucpgfkqahzddi37ktceo3ah7ngmcopnpyyd7:80", "failed to parse Multiaddress /onion3/vww6ybal4bd7szmgncyruucpgfkqahzddi37ktceo3ah7ngmcopnpyyd7:80: failed to parse onion address: vww6ybal4bd7szmgncyruucpgfkqahzddi37ktceo3ah7ngmcopnpyyd7:80 not a Tor onion address."),
            Tuple2("/onion3/vww6ybal4bd7szmgncyruucpgfkqahzddi37ktceo3ah7ngmcopnpyyd:0", "failed to parse Multiaddress /onion3/vww6ybal4bd7szmgncyruucpgfkqahzddi37ktceo3ah7ngmcopnpyyd:0: Port number is not in range(1, 65536): 0"),
            Tuple2("/onion3/vww6ybal4bd7szmgncyruucpgfkqahzddi37ktceo3ah7ngmcopnpyyd:-1", "failed to parse Multiaddress /onion3/vww6ybal4bd7szmgncyruucpgfkqahzddi37ktceo3ah7ngmcopnpyyd:-1: Port number is not in range(1, 65536): -1"),
            Tuple2("/onion3/vww6ybal4bd7szmgncyruucpgfkqahzddi37ktceo3ah7ngmcopnpyyd", "failed to parse Multiaddress /onion3/vww6ybal4bd7szmgncyruucpgfkqahzddi37ktceo3ah7ngmcopnpyyd: failed to parse onion address: vww6ybal4bd7szmgncyruucpgfkqahzddi37ktceo3ah7ngmcopnpyyd does not contain a port number"),
            Tuple2("/onion3/vww6ybal4bd7szmgncyruucpgfkqahzddi37ktceo3ah7ngmcopnpyy@:567", "failed to parse Multiaddress /onion3/vww6ybal4bd7szmgncyruucpgfkqahzddi37ktceo3ah7ngmcopnpyy@:567: Invalid onion address host: vww6ybal4bd7szmgncyruucpgfkqahzddi37ktceo3ah7ngmcopnpyy@"),
            Tuple2(
                "/garlic64/jT~IyXaoauTni6N4517EG8mrFUKpy0IlgZh-EY9csMAk82Odatmzr~YTZy8Hv7u~wvkg75EFNOyqb~nAPg-khyp2TS~ObUz8WlqYAM2VlEzJ7wJB91P-cUlKF18zSzVoJFmsrcQHZCirSbWoOknS6iNmsGRh5KVZsBEfp1Dg3gwTipTRIx7Vl5Vy~1OSKQVjYiGZS9q8RL0MF~7xFiKxZDLbPxk0AK9TzGGqm~wMTI2HS0Gm4Ycy8LYPVmLvGonIBYndg2bJC7WLuF6tVjVquiokSVDKFwq70BCUU5AU-EvdOD5KEOAM7mPfw-gJUG4tm1TtvcobrObqoRnmhXPTBTN5H7qDD12AvlwFGnfAlBXjuP4xOUAISL5SRLiulrsMSiT4GcugSI80mF6sdB0zWRgL1yyvoVWeTBn1TqjO27alr95DGTluuSqrNAxgpQzCKEWAyzrQkBfo2avGAmmz2NaHaAvYbOg0QSJz1PLjv2jdPW~ofiQmrGWM1cd~1cCqAAAA7:80",
                "failed to parse Multiaddress /garlic64/jT~IyXaoauTni6N4517EG8mrFUKpy0IlgZh-EY9csMAk82Odatmzr~YTZy8Hv7u~wvkg75EFNOyqb~nAPg-khyp2TS~ObUz8WlqYAM2VlEzJ7wJB91P-cUlKF18zSzVoJFmsrcQHZCirSbWoOknS6iNmsGRh5KVZsBEfp1Dg3gwTipTRIx7Vl5Vy~1OSKQVjYiGZS9q8RL0MF~7xFiKxZDLbPxk0AK9TzGGqm~wMTI2HS0Gm4Ycy8LYPVmLvGonIBYndg2bJC7WLuF6tVjVquiokSVDKFwq70BCUU5AU-EvdOD5KEOAM7mPfw-gJUG4tm1TtvcobrObqoRnmhXPTBTN5H7qDD12AvlwFGnfAlBXjuP4xOUAISL5SRLiulrsMSiT4GcugSI80mF6sdB0zWRgL1yyvoVWeTBn1TqjO27alr95DGTluuSqrNAxgpQzCKEWAyzrQkBfo2avGAmmz2NaHaAvYbOg0QSJz1PLjv2jdPW~ofiQmrGWM1cd~1cCqAAAA7:80: Invalid garlic addr: jT~IyXaoauTni6N4... Could not decode Multibase"
            ),
            Tuple2(
                "/garlic64/jT~IyXaoauTni6N4517EG8mrFUKpy0IlgZh-EY9csMAk82Odatmzr~YTZy8Hv7u~wvkg75EFNOyqb~nAPg-khyp2TS~ObUz8WlqYAM2VlEzJ7wJB91P-cUlKF18zSzVoJFmsrcQHZCirSbWoOknS6iNmsGRh5KVZsBEfp1Dg3gwTipTRIx7Vl5Vy~1OSKQVjYiGZS9q8RL0MF~7xFiKxZDLbPxk0AK9TzGGqm~wMTI2HS0Gm4Ycy8LYPVmLvGonIBYndg2bJC7WLuF6tVjVquiokSVDKFwq70BCUU5AU-EvdOD5KEOAM7mPfw-gJUG4tm1TtvcobrObqoRnmhXPTBTN5H7qDD12AvlwFGnfAlBXjuP4xOUAISL5SRLiulrsMSiT4GcugSI80mF6sdB0zWRgL1yyvoVWeTBn1TqjO27alr95DGTluuSqrNAxgpQzCKEWAyzrQkBfo2avGAmmz2NaHaAvYbOg0QSJz1PLjv2jdPW~ofiQmrGWM1cd~1cCqAAAA:0",
                "failed to parse Multiaddress /garlic64/jT~IyXaoauTni6N4517EG8mrFUKpy0IlgZh-EY9csMAk82Odatmzr~YTZy8Hv7u~wvkg75EFNOyqb~nAPg-khyp2TS~ObUz8WlqYAM2VlEzJ7wJB91P-cUlKF18zSzVoJFmsrcQHZCirSbWoOknS6iNmsGRh5KVZsBEfp1Dg3gwTipTRIx7Vl5Vy~1OSKQVjYiGZS9q8RL0MF~7xFiKxZDLbPxk0AK9TzGGqm~wMTI2HS0Gm4Ycy8LYPVmLvGonIBYndg2bJC7WLuF6tVjVquiokSVDKFwq70BCUU5AU-EvdOD5KEOAM7mPfw-gJUG4tm1TtvcobrObqoRnmhXPTBTN5H7qDD12AvlwFGnfAlBXjuP4xOUAISL5SRLiulrsMSiT4GcugSI80mF6sdB0zWRgL1yyvoVWeTBn1TqjO27alr95DGTluuSqrNAxgpQzCKEWAyzrQkBfo2avGAmmz2NaHaAvYbOg0QSJz1PLjv2jdPW~ofiQmrGWM1cd~1cCqAAAA:0: Invalid garlic addr: jT~IyXaoauTni6N4... Could not decode Multibase"
            ),
            Tuple2(
                "/garlic64/jT~IyXaoauTni6N4517EG8mrFUKpy0IlgZh-EY9csMAk82Odatmzr~YTZy8Hv7u~wvkg75EFNOyqb~nAPg-khyp2TS~ObUz8WlqYAM2VlEzJ7wJB91P-cUlKF18zSzVoJFmsrcQHZCirSbWoOknS6iNmsGRh5KVZsBEfp1Dg3gwTipTRIx7Vl5Vy~1OSKQVjYiGZS9q8RL0MF~7xFiKxZDLbPxk0AK9TzGGqm~wMTI2HS0Gm4Ycy8LYPVmLvGonIBYndg2bJC7WLuF6tVjVquiokSVDKFwq70BCUU5AU-EvdOD5KEOAM7mPfw-gJUG4tm1TtvcobrObqoRnmhXPTBTN5H7qDD12AvlwFGnfAlBXjuP4xOUAISL5SRLiulrsMSiT4GcugSI80mF6sdB0zWRgL1yyvoVWeTBn1TqjO27alr95DGTluuSqrNAxgpQzCKEWAyzrQkBfo2avGAmmz2NaHaAvYbOg0QSJz1PLjv2jdPW~ofiQmrGWM1cd~1cCqAAAA:0",
                "failed to parse Multiaddress /garlic64/jT~IyXaoauTni6N4517EG8mrFUKpy0IlgZh-EY9csMAk82Odatmzr~YTZy8Hv7u~wvkg75EFNOyqb~nAPg-khyp2TS~ObUz8WlqYAM2VlEzJ7wJB91P-cUlKF18zSzVoJFmsrcQHZCirSbWoOknS6iNmsGRh5KVZsBEfp1Dg3gwTipTRIx7Vl5Vy~1OSKQVjYiGZS9q8RL0MF~7xFiKxZDLbPxk0AK9TzGGqm~wMTI2HS0Gm4Ycy8LYPVmLvGonIBYndg2bJC7WLuF6tVjVquiokSVDKFwq70BCUU5AU-EvdOD5KEOAM7mPfw-gJUG4tm1TtvcobrObqoRnmhXPTBTN5H7qDD12AvlwFGnfAlBXjuP4xOUAISL5SRLiulrsMSiT4GcugSI80mF6sdB0zWRgL1yyvoVWeTBn1TqjO27alr95DGTluuSqrNAxgpQzCKEWAyzrQkBfo2avGAmmz2NaHaAvYbOg0QSJz1PLjv2jdPW~ofiQmrGWM1cd~1cCqAAAA:0: Invalid garlic addr: jT~IyXaoauTni6N4... Could not decode Multibase"
            ),
            Tuple2(
                "/garlic64/jT~IyXaoauTni6N4517EG8mrFUKpy0IlgZh-EY9csMAk82Odatmzr~YTZy8Hv7u~wvkg75EFNOyqb~nAPg-khyp2TS~ObUz8WlqYAM2VlEzJ7wJB91P-cUlKF18zSzVoJFmsrcQHZCirSbWoOknS6iNmsGRh5KVZsBEfp1Dg3gwTipTRIx7Vl5Vy~1OSKQVjYiGZS9q8RL0MF~7xFiKxZDLbPxk0AK9TzGGqm~wMTI2HS0Gm4Ycy8LYPVmLvGonIBYndg2bJC7WLuF6tVjVquiokSVDKFwq70BCUU5AU-EvdOD5KEOAM7mPfw-gJUG4tm1TtvcobrObqoRnmhXPTBTN5H7qDD12AvlwFGnfAlBXjuP4xOUAISL5SRLiulrsMSiT4GcugSI80mF6sdB0zWRgL1yyvoVWeTBn1TqjO27alr95DGTluuSqrNAxgpQzCKEWAyzrQkBfo2avGAmmz2NaHaAvYbOg0QSJz1PLjv2jdPW~ofiQmrGWM1cd~1cCqAAAA:-1",
                "failed to parse Multiaddress /garlic64/jT~IyXaoauTni6N4517EG8mrFUKpy0IlgZh-EY9csMAk82Odatmzr~YTZy8Hv7u~wvkg75EFNOyqb~nAPg-khyp2TS~ObUz8WlqYAM2VlEzJ7wJB91P-cUlKF18zSzVoJFmsrcQHZCirSbWoOknS6iNmsGRh5KVZsBEfp1Dg3gwTipTRIx7Vl5Vy~1OSKQVjYiGZS9q8RL0MF~7xFiKxZDLbPxk0AK9TzGGqm~wMTI2HS0Gm4Ycy8LYPVmLvGonIBYndg2bJC7WLuF6tVjVquiokSVDKFwq70BCUU5AU-EvdOD5KEOAM7mPfw-gJUG4tm1TtvcobrObqoRnmhXPTBTN5H7qDD12AvlwFGnfAlBXjuP4xOUAISL5SRLiulrsMSiT4GcugSI80mF6sdB0zWRgL1yyvoVWeTBn1TqjO27alr95DGTluuSqrNAxgpQzCKEWAyzrQkBfo2avGAmmz2NaHaAvYbOg0QSJz1PLjv2jdPW~ofiQmrGWM1cd~1cCqAAAA:-1: Invalid garlic addr: jT~IyXaoauTni6N4... Could not decode Multibase"
            ),
            Tuple2(
                "/garlic64/jT~IyXaoauTni6N4517EG8mrFUKpy0IlgZh-EY9csMAk82Odatmzr~YTZy8Hv7u~wvkg75EFNOyqb~nAPg-khyp2TS~ObUz8WlqYAM2VlEzJ7wJB91P-cUlKF18zSzVoJFmsrcQHZCirSbWoOknS6iNmsGRh5KVZsBEfp1Dg3gwTipTRIx7Vl5Vy~1OSKQVjYiGZS9q8RL0MF~7xFiKxZDLbPxk0AK9TzGGqm~wMTI2HS0Gm4Ycy8LYPVmLvGonIBYndg2bJC7WLuF6tVjVquiokSVDKFwq70BCUU5AU-EvdOD5KEOAM7mPfw-gJUG4tm1TtvcobrObqoRnmhXPTBTN5H7qDD12AvlwFGnfAlBXjuP4xOUAISL5SRLiulrsMSiT4GcugSI80mF6sdB0zWRgL1yyvoVWeTBn1TqjO27alr95DGTluuSqrNAxgpQzCKEWAyzrQkBfo2avGAmmz2NaHaAvYbOg0QSJz1PLjv2jdPW~ofiQmrGWM1cd~1cCqAAAA@:567",
                "failed to parse Multiaddress /garlic64/jT~IyXaoauTni6N4517EG8mrFUKpy0IlgZh-EY9csMAk82Odatmzr~YTZy8Hv7u~wvkg75EFNOyqb~nAPg-khyp2TS~ObUz8WlqYAM2VlEzJ7wJB91P-cUlKF18zSzVoJFmsrcQHZCirSbWoOknS6iNmsGRh5KVZsBEfp1Dg3gwTipTRIx7Vl5Vy~1OSKQVjYiGZS9q8RL0MF~7xFiKxZDLbPxk0AK9TzGGqm~wMTI2HS0Gm4Ycy8LYPVmLvGonIBYndg2bJC7WLuF6tVjVquiokSVDKFwq70BCUU5AU-EvdOD5KEOAM7mPfw-gJUG4tm1TtvcobrObqoRnmhXPTBTN5H7qDD12AvlwFGnfAlBXjuP4xOUAISL5SRLiulrsMSiT4GcugSI80mF6sdB0zWRgL1yyvoVWeTBn1TqjO27alr95DGTluuSqrNAxgpQzCKEWAyzrQkBfo2avGAmmz2NaHaAvYbOg0QSJz1PLjv2jdPW~ofiQmrGWM1cd~1cCqAAAA@:567: Invalid garlic addr: jT~IyXaoauTni6N4... Could not decode Multibase"
            ),
            Tuple2(
                "/garlic64/jT~IyXaoauTni6N4517EG8mrFUKpy0IlgZh-EY9csMAk82Odatmzr~YTZy8Hv7u~wvkg75EFNOyqb~nAPg-khyp2TS~ObUz8WlqYAM2VlEzJ7wJB91P-cUlKF18zSzVoJFmsrcQHZCirSbWoOknS6iNmsGRh5KVZsBEfp1Dg3gwTipTRIx7Vl5Vy~1OSKQVjYiGZS9q8RL0MF~7xFiKxZDLbPxk0AK9TzGGqm~wMTI2HS0Gm4Ycy8LYPVmLvGonIBYndg2bJC7WLuF6tVjVquiokSVDKFwq70BCUU5AU-EvdOD5KEOAM7mPfw-gJUG4tm1TtvcobrObqoRnmhXPTBTN5H7qDD12AvlwFGnfAlBXjuP4xOUAISL5SRLiulrsMSiT4GcugSI80mF6sdB0zWRgL1yyvoVWeTBn1TqjO27alr95DGTluuSqrNAxgpQzCKEWAyzrQkBfo2avGAmmz2NaHaAvYbOg0QSJz1PLjv2jdPW~ofiQmrGWM1cd~1cCqAAAA7:80",
                "failed to parse Multiaddress /garlic64/jT~IyXaoauTni6N4517EG8mrFUKpy0IlgZh-EY9csMAk82Odatmzr~YTZy8Hv7u~wvkg75EFNOyqb~nAPg-khyp2TS~ObUz8WlqYAM2VlEzJ7wJB91P-cUlKF18zSzVoJFmsrcQHZCirSbWoOknS6iNmsGRh5KVZsBEfp1Dg3gwTipTRIx7Vl5Vy~1OSKQVjYiGZS9q8RL0MF~7xFiKxZDLbPxk0AK9TzGGqm~wMTI2HS0Gm4Ycy8LYPVmLvGonIBYndg2bJC7WLuF6tVjVquiokSVDKFwq70BCUU5AU-EvdOD5KEOAM7mPfw-gJUG4tm1TtvcobrObqoRnmhXPTBTN5H7qDD12AvlwFGnfAlBXjuP4xOUAISL5SRLiulrsMSiT4GcugSI80mF6sdB0zWRgL1yyvoVWeTBn1TqjO27alr95DGTluuSqrNAxgpQzCKEWAyzrQkBfo2avGAmmz2NaHaAvYbOg0QSJz1PLjv2jdPW~ofiQmrGWM1cd~1cCqAAAA7:80: Invalid garlic addr: jT~IyXaoauTni6N4... Could not decode Multibase"
            ),
            Tuple2(
                "/garlic64/jT~IyXaoauTni6N4517EG8mrFUKpy0IlgZh-EY9csMAk82Odatmzr~YTZy8Hv7u~wvkg75EFNOyqb~nAPg-khyp2TS~ObUz8WlqYAM2VlEzJ7wJB91P-cUlKF18zSzVoJFmsrcQHZCirSbWoOknS6iNmsGRh5KVZsBEfp1Dg3gwTipTRIx7Vl5Vy~1OSKQVjYiGZS9q8RL0MF~7xFiKxZDLbPxk0AK9TzGGqm~wMTI2HS0Gm4Ycy8LYPVmLvGonIBYndg2bJC7WLuF6tVjVquiokSVDKFwq70BCUU5AU-EvdOD5KEOAM7mPfw-gJUG4tm1TtvcobrObqoRnmhXPTBTN5H7qDD12AvlwFGnfAlBXjuP4xOUAISL5SRLiulrsMSiT4GcugSI80mF6sdB0zWRgL1yyvoVWeTBn1TqjO27alr95DGTluuSqrNAxgpQzCKEWAyzrQkBfo2avGAmmz2NaHaAvYbOg0QSJz1PLjv2jdPW~ofiQmrGWM1cd~1cCqAAAA:0",
                "failed to parse Multiaddress /garlic64/jT~IyXaoauTni6N4517EG8mrFUKpy0IlgZh-EY9csMAk82Odatmzr~YTZy8Hv7u~wvkg75EFNOyqb~nAPg-khyp2TS~ObUz8WlqYAM2VlEzJ7wJB91P-cUlKF18zSzVoJFmsrcQHZCirSbWoOknS6iNmsGRh5KVZsBEfp1Dg3gwTipTRIx7Vl5Vy~1OSKQVjYiGZS9q8RL0MF~7xFiKxZDLbPxk0AK9TzGGqm~wMTI2HS0Gm4Ycy8LYPVmLvGonIBYndg2bJC7WLuF6tVjVquiokSVDKFwq70BCUU5AU-EvdOD5KEOAM7mPfw-gJUG4tm1TtvcobrObqoRnmhXPTBTN5H7qDD12AvlwFGnfAlBXjuP4xOUAISL5SRLiulrsMSiT4GcugSI80mF6sdB0zWRgL1yyvoVWeTBn1TqjO27alr95DGTluuSqrNAxgpQzCKEWAyzrQkBfo2avGAmmz2NaHaAvYbOg0QSJz1PLjv2jdPW~ofiQmrGWM1cd~1cCqAAAA:0: Invalid garlic addr: jT~IyXaoauTni6N4... Could not decode Multibase"
            ),
            Tuple2(
                "/garlic64/jT~IyXaoauTni6N4517EG8mrFUKpy0IlgZh-EY9csMAk82Odatmzr~YTZy8Hv7u~wvkg75EFNOyqb~nAPg-khyp2TS~ObUz8WlqYAM2VlEzJ7wJB91P-cUlKF18zSzVoJFmsrcQHZCirSbWoOknS6iNmsGRh5KVZsBEfp1Dg3gwTipTRIx7Vl5Vy~1OSKQVjYiGZS9q8RL0MF~7xFiKxZDLbPxk0AK9TzGGqm~wMTI2HS0Gm4Ycy8LYPVmLvGonIBYndg2bJC7WLuF6tVjVquiokSVDKFwq70BCUU5AU-EvdOD5KEOAM7mPfw-gJUG4tm1TtvcobrObqoRnmhXPTBTN5H7qDD12AvlwFGnfAlBXjuP4xOUAISL5SRLiulrsMSiT4GcugSI80mF6sdB0zWRgL1yyvoVWeTBn1TqjO27alr95DGTluuSqrNAxgpQzCKEWAyzrQkBfo2avGAmmz2NaHaAvYbOg0QSJz1PLjv2jdPW~ofiQmrGWM1cd~1cCqAAAA:0",
                "failed to parse Multiaddress /garlic64/jT~IyXaoauTni6N4517EG8mrFUKpy0IlgZh-EY9csMAk82Odatmzr~YTZy8Hv7u~wvkg75EFNOyqb~nAPg-khyp2TS~ObUz8WlqYAM2VlEzJ7wJB91P-cUlKF18zSzVoJFmsrcQHZCirSbWoOknS6iNmsGRh5KVZsBEfp1Dg3gwTipTRIx7Vl5Vy~1OSKQVjYiGZS9q8RL0MF~7xFiKxZDLbPxk0AK9TzGGqm~wMTI2HS0Gm4Ycy8LYPVmLvGonIBYndg2bJC7WLuF6tVjVquiokSVDKFwq70BCUU5AU-EvdOD5KEOAM7mPfw-gJUG4tm1TtvcobrObqoRnmhXPTBTN5H7qDD12AvlwFGnfAlBXjuP4xOUAISL5SRLiulrsMSiT4GcugSI80mF6sdB0zWRgL1yyvoVWeTBn1TqjO27alr95DGTluuSqrNAxgpQzCKEWAyzrQkBfo2avGAmmz2NaHaAvYbOg0QSJz1PLjv2jdPW~ofiQmrGWM1cd~1cCqAAAA:0: Invalid garlic addr: jT~IyXaoauTni6N4... Could not decode Multibase"
            ),
            Tuple2(
                "/garlic64/jT~IyXaoauTni6N4517EG8mrFUKpy0IlgZh-EY9csMAk82Odatmzr~YTZy8Hv7u~wvkg75EFNOyqb~nAPg-khyp2TS~ObUz8WlqYAM2VlEzJ7wJB91P-cUlKF18zSzVoJFmsrcQHZCirSbWoOknS6iNmsGRh5KVZsBEfp1Dg3gwTipTRIx7Vl5Vy~1OSKQVjYiGZS9q8RL0MF~7xFiKxZDLbPxk0AK9TzGGqm~wMTI2HS0Gm4Ycy8LYPVmLvGonIBYndg2bJC7WLuF6tVjVquiokSVDKFwq70BCUU5AU-EvdOD5KEOAM7mPfw-gJUG4tm1TtvcobrObqoRnmhXPTBTN5H7qDD12AvlwFGnfAlBXjuP4xOUAISL5SRLiulrsMSiT4GcugSI80mF6sdB0zWRgL1yyvoVWeTBn1TqjO27alr95DGTluuSqrNAxgpQzCKEWAyzrQkBfo2avGAmmz2NaHaAvYbOg0QSJz1PLjv2jdPW~ofiQmrGWM1cd~1cCqAAAA:-1",
                "failed to parse Multiaddress /garlic64/jT~IyXaoauTni6N4517EG8mrFUKpy0IlgZh-EY9csMAk82Odatmzr~YTZy8Hv7u~wvkg75EFNOyqb~nAPg-khyp2TS~ObUz8WlqYAM2VlEzJ7wJB91P-cUlKF18zSzVoJFmsrcQHZCirSbWoOknS6iNmsGRh5KVZsBEfp1Dg3gwTipTRIx7Vl5Vy~1OSKQVjYiGZS9q8RL0MF~7xFiKxZDLbPxk0AK9TzGGqm~wMTI2HS0Gm4Ycy8LYPVmLvGonIBYndg2bJC7WLuF6tVjVquiokSVDKFwq70BCUU5AU-EvdOD5KEOAM7mPfw-gJUG4tm1TtvcobrObqoRnmhXPTBTN5H7qDD12AvlwFGnfAlBXjuP4xOUAISL5SRLiulrsMSiT4GcugSI80mF6sdB0zWRgL1yyvoVWeTBn1TqjO27alr95DGTluuSqrNAxgpQzCKEWAyzrQkBfo2avGAmmz2NaHaAvYbOg0QSJz1PLjv2jdPW~ofiQmrGWM1cd~1cCqAAAA:-1: Invalid garlic addr: jT~IyXaoauTni6N4... Could not decode Multibase"
            ),
            Tuple2(
                "/garlic64/jT~IyXaoauTni6N4517EG8mrFUKpy0IlgZh-EY9csMAk82Odatmzr~YTZy8Hv7u~wvkg75EFNOyqb~nAPg-khyp2TS~ObUz8WlqYAM2VlEzJ7wJB91P-cUlKF18zSzVoJFmsrcQHZCirSbWoOknS6iNmsGRh5KVZsBEfp1Dg3gwTipTRIx7Vl5Vy~1OSKQVjYiGZS9q8RL0MF~7xFiKxZDLbPxk0AK9TzGGqm~wMTI2HS0Gm4Ycy8LYPVmLvGonIBYndg2bJC7WLuF6tVjVquiokSVDKFwq70BCUU5AU-EvdOD5KEOAM7mPfw-gJUG4tm1TtvcobrObqoRnmhXPTBTN5H7qDD12AvlwFGnfAlBXjuP4xOUAISL5SRLiulrsMSiT4GcugSI80mF6sdB0zWRgL1yyvoVWeTBn1TqjO27alr95DGTluuSqrNAxgpQzCKEWAyzrQkBfo2avGAmmz2NaHaAvYbOg0QSJz1PLjv2jdPW~ofiQmrGWM1cd~1cCqAAAA@:567",
                "failed to parse Multiaddress /garlic64/jT~IyXaoauTni6N4517EG8mrFUKpy0IlgZh-EY9csMAk82Odatmzr~YTZy8Hv7u~wvkg75EFNOyqb~nAPg-khyp2TS~ObUz8WlqYAM2VlEzJ7wJB91P-cUlKF18zSzVoJFmsrcQHZCirSbWoOknS6iNmsGRh5KVZsBEfp1Dg3gwTipTRIx7Vl5Vy~1OSKQVjYiGZS9q8RL0MF~7xFiKxZDLbPxk0AK9TzGGqm~wMTI2HS0Gm4Ycy8LYPVmLvGonIBYndg2bJC7WLuF6tVjVquiokSVDKFwq70BCUU5AU-EvdOD5KEOAM7mPfw-gJUG4tm1TtvcobrObqoRnmhXPTBTN5H7qDD12AvlwFGnfAlBXjuP4xOUAISL5SRLiulrsMSiT4GcugSI80mF6sdB0zWRgL1yyvoVWeTBn1TqjO27alr95DGTluuSqrNAxgpQzCKEWAyzrQkBfo2avGAmmz2NaHaAvYbOg0QSJz1PLjv2jdPW~ofiQmrGWM1cd~1cCqAAAA@:567: Invalid garlic addr: jT~IyXaoauTni6N4... Could not decode Multibase"
            ),
            Tuple2("/garlic32/566niximlxdzpanmn4qouucvua3k7neniwss47li5r6ugoertzu", "failed to parse Multiaddress /garlic32/566niximlxdzpanmn4qouucvua3k7neniwss47li5r6ugoertzu: Invalid garlic addr: 566niximlxdzpanmn4qouucvua3k7neniwss47li5r6ugoertzu not a i2p base32 address. len: 51"),
            Tuple2("/garlic32/566niximlxdzpanmn4qouucvua3k7neniwss47li5r6ugoertzu77", "failed to parse Multiaddress /garlic32/566niximlxdzpanmn4qouucvua3k7neniwss47li5r6ugoertzu77: Invalid garlic addr: 566niximlxdzpanmn4qouucvua3k7neniwss47li5r6ugoertzu77 not a i2p base32 address. len: 53"),
            Tuple2("/garlic32/566niximlxdzpanmn4qouucvua3k7neniwss47li5r6ugoertzu:80", "failed to parse Multiaddress /garlic32/566niximlxdzpanmn4qouucvua3k7neniwss47li5r6ugoertzu:80: Invalid garlic addr: 566niximlxdzpanmn4qouucvua3k7neniwss47li5r6ugoertzu:80 not a i2p base32 address. len: 54"),
            Tuple2("/garlic32/566niximlxdzpanmn4qouucvua3k7neniwss47li5r6ugoertzuq:-1", "failed to parse Multiaddress /garlic32/566niximlxdzpanmn4qouucvua3k7neniwss47li5r6ugoertzuq:-1: invalid value 566niximlxdzpanmn4qouucvua3k7neniwss47li5r6ugoertzuq:-1 for protocol Garlic32: illegal base32 data at input byte 52"),
            Tuple2("/garlic32/566niximlxdzpanmn4qouucvua3k7neniwss47li5r6ugoertzu@", "failed to parse Multiaddress /garlic32/566niximlxdzpanmn4qouucvua3k7neniwss47li5r6ugoertzu@: invalid value 566niximlxdzpanmn4qouucvua3k7neniwss47li5r6ugoertzu@ for protocol Garlic32: illegal base32 data at input byte 51"),
            Tuple2("/udp/1234/sctp", "failed to parse Multiaddress /udp/1234/sctp: unexpected end of Multiaddress"),
            Tuple2("/udp/1234/udt/1234", "failed to parse Multiaddress /udp/1234/udt/1234: no protocol with name 1234"),
            Tuple2("/udp/1234/utp/1234", "failed to parse Multiaddress /udp/1234/utp/1234: no protocol with name 1234"),
            Tuple2("/ip4/127.0.0.1/udp/jfodsajfidosajfoidsa", "failed to parse Multiaddress /ip4/127.0.0.1/udp/jfodsajfidosajfoidsa: Failed to parse address jfodsajfidosajfoidsa"),
            Tuple2("/ip4/127.0.0.1/udp", "failed to parse Multiaddress /ip4/127.0.0.1/udp: unexpected end of Multiaddress"),
            Tuple2("/ip4/127.0.0.1/tcp/jfodsajfidosajfoidsa", "failed to parse Multiaddress /ip4/127.0.0.1/tcp/jfodsajfidosajfoidsa: Failed to parse address jfodsajfidosajfoidsa"),
            Tuple2("/ip4/127.0.0.1/tcp", "failed to parse Multiaddress /ip4/127.0.0.1/tcp: unexpected end of Multiaddress"),
            Tuple2("/ip4/127.0.0.1/quic/1234", "failed to parse Multiaddress /ip4/127.0.0.1/quic/1234: no protocol with name 1234"),
            Tuple2("/ip4/127.0.0.1/udp/1234/quic/webtransport/certhash", "failed to parse Multiaddress /ip4/127.0.0.1/udp/1234/quic/webtransport/certhash: unexpected end of Multiaddress"),
            Tuple2("/ip4/127.0.0.1/udp/1234/quic/webtransport/certhash/b2uaraocy6yrdblb4sfptaddgimjmmp", "failed to parse Multiaddress /ip4/127.0.0.1/udp/1234/quic/webtransport/certhash/b2uaraocy6yrdblb4sfptaddgimjmmp: length greater than remaining number of bytes in buffer"), // 1 character missing from certhash
            Tuple2("/ip4/127.0.0.1/ipfs", "failed to parse Multiaddress /ip4/127.0.0.1/ipfs: unexpected end of Multiaddress"),
            Tuple2("/ip4/127.0.0.1/ipfs/tcp", "failed to parse Multiaddress /ip4/127.0.0.1/ipfs/tcp: failed to parse p2p address tcp: illegal base32 data at input byte 0"),
            Tuple2("/ip4/127.0.0.1/p2p", "failed to parse Multiaddress /ip4/127.0.0.1/p2p: unexpected end of Multiaddress"),
            Tuple2("/ip4/127.0.0.1/p2p/tcp", "failed to parse Multiaddress /ip4/127.0.0.1/p2p/tcp: failed to parse p2p address tcp: illegal base32 data at input byte 0"),
            Tuple2("/unix", "failed to parse Multiaddress /unix: unexpected end of Multiaddress"),
            Tuple2("/ip4/1.2.3.4/tcp/80/unix", "failed to parse Multiaddress /ip4/1.2.3.4/tcp/80/unix: unexpected end of Multiaddress"),
            Tuple2("/ip4/127.0.0.1/tcp/9090/http/p2p-webcrt-direct", "failed to parse Multiaddress /ip4/127.0.0.1/tcp/9090/http/p2p-webcrt-direct: no protocol with name p2p-webcrt-direct")
        ).map { (multiaddress, message) ->
            DynamicTest.dynamicTest("Test: $multiaddress") {
                assertErrorResult(message) { Multiaddress.fromString(multiaddress) }
            }
        }.stream()
    }

    @TestFactory
    fun `construction succeeds`(): Stream<DynamicTest> {
        return listOf(
            "/ip4/1.2.3.4",
            "/ip4/0.0.0.0",
            "/ip4/192.0.2.0/ipcidr/24",
            "/ip4/192.0.2.0/ipcidr/255",
            "/ip6/::1",
            "/ip6/2601:9:4f81:9700:803e:ca65:66e8:c21",
            "/ip6/2601:9:4f81:9700:803e:ca65:66e8:c21/udp/1234/quic",
            "/ip6zone/x/ip6/fe80::1",
            "/ip6zone/x%y/ip6/fe80::1",
            "/ip6zone/x%y/ip6/::",
            "/ip6zone/x/ip6/fe80::1/udp/1234/quic",
            "/onion/timaq4ygg2iegci7:1234",
            "/onion/timaq4ygg2iegci7:80/http",
            "/onion3/vww6ybal4bd7szmgncyruucpgfkqahzddi37ktceo3ah7ngmcopnpyyd:1234",
            "/onion3/vww6ybal4bd7szmgncyruucpgfkqahzddi37ktceo3ah7ngmcopnpyyd:80/http",
            "/garlic64/jT~IyXaoauTni6N4517EG8mrFUKpy0IlgZh-EY9csMAk82Odatmzr~YTZy8Hv7u~wvkg75EFNOyqb~nAPg-khyp2TS~ObUz8WlqYAM2VlEzJ7wJB91P-cUlKF18zSzVoJFmsrcQHZCirSbWoOknS6iNmsGRh5KVZsBEfp1Dg3gwTipTRIx7Vl5Vy~1OSKQVjYiGZS9q8RL0MF~7xFiKxZDLbPxk0AK9TzGGqm~wMTI2HS0Gm4Ycy8LYPVmLvGonIBYndg2bJC7WLuF6tVjVquiokSVDKFwq70BCUU5AU-EvdOD5KEOAM7mPfw-gJUG4tm1TtvcobrObqoRnmhXPTBTN5H7qDD12AvlwFGnfAlBXjuP4xOUAISL5SRLiulrsMSiT4GcugSI80mF6sdB0zWRgL1yyvoVWeTBn1TqjO27alr95DGTluuSqrNAxgpQzCKEWAyzrQkBfo2avGAmmz2NaHaAvYbOg0QSJz1PLjv2jdPW~ofiQmrGWM1cd~1cCqAAAA",
            "/garlic64/jT~IyXaoauTni6N4517EG8mrFUKpy0IlgZh-EY9csMAk82Odatmzr~YTZy8Hv7u~wvkg75EFNOyqb~nAPg-khyp2TS~ObUz8WlqYAM2VlEzJ7wJB91P-cUlKF18zSzVoJFmsrcQHZCirSbWoOknS6iNmsGRh5KVZsBEfp1Dg3gwTipTRIx7Vl5Vy~1OSKQVjYiGZS9q8RL0MF~7xFiKxZDLbPxk0AK9TzGGqm~wMTI2HS0Gm4Ycy8LYPVmLvGonIBYndg2bJC7WLuF6tVjVquiokSVDKFwq70BCUU5AU-EvdOD5KEOAM7mPfw-gJUG4tm1TtvcobrObqoRnmhXPTBTN5H7qDD12AvlwFGnfAlBXjuP4xOUAISL5SRLiulrsMSiT4GcugSI80mF6sdB0zWRgL1yyvoVWeTBn1TqjO27alr95DGTluuSqrNAxgpQzCKEWAyzrQkBfo2avGAmmz2NaHaAvYbOg0QSJz1PLjv2jdPW~ofiQmrGWM1cd~1cCqAAAA/http",
            "/garlic64/jT~IyXaoauTni6N4517EG8mrFUKpy0IlgZh-EY9csMAk82Odatmzr~YTZy8Hv7u~wvkg75EFNOyqb~nAPg-khyp2TS~ObUz8WlqYAM2VlEzJ7wJB91P-cUlKF18zSzVoJFmsrcQHZCirSbWoOknS6iNmsGRh5KVZsBEfp1Dg3gwTipTRIx7Vl5Vy~1OSKQVjYiGZS9q8RL0MF~7xFiKxZDLbPxk0AK9TzGGqm~wMTI2HS0Gm4Ycy8LYPVmLvGonIBYndg2bJC7WLuF6tVjVquiokSVDKFwq70BCUU5AU-EvdOD5KEOAM7mPfw-gJUG4tm1TtvcobrObqoRnmhXPTBTN5H7qDD12AvlwFGnfAlBXjuP4xOUAISL5SRLiulrsMSiT4GcugSI80mF6sdB0zWRgL1yyvoVWeTBn1TqjO27alr95DGTluuSqrNAxgpQzCKEWAyzrQkBfo2avGAmmz2NaHaAvYbOg0QSJz1PLjv2jdPW~ofiQmrGWM1cd~1cCqAAAA/udp/8080",
            "/garlic64/jT~IyXaoauTni6N4517EG8mrFUKpy0IlgZh-EY9csMAk82Odatmzr~YTZy8Hv7u~wvkg75EFNOyqb~nAPg-khyp2TS~ObUz8WlqYAM2VlEzJ7wJB91P-cUlKF18zSzVoJFmsrcQHZCirSbWoOknS6iNmsGRh5KVZsBEfp1Dg3gwTipTRIx7Vl5Vy~1OSKQVjYiGZS9q8RL0MF~7xFiKxZDLbPxk0AK9TzGGqm~wMTI2HS0Gm4Ycy8LYPVmLvGonIBYndg2bJC7WLuF6tVjVquiokSVDKFwq70BCUU5AU-EvdOD5KEOAM7mPfw-gJUG4tm1TtvcobrObqoRnmhXPTBTN5H7qDD12AvlwFGnfAlBXjuP4xOUAISL5SRLiulrsMSiT4GcugSI80mF6sdB0zWRgL1yyvoVWeTBn1TqjO27alr95DGTluuSqrNAxgpQzCKEWAyzrQkBfo2avGAmmz2NaHaAvYbOg0QSJz1PLjv2jdPW~ofiQmrGWM1cd~1cCqAAAA/tcp/8080",
            "/garlic32/566niximlxdzpanmn4qouucvua3k7neniwss47li5r6ugoertzuq",
            "/garlic32/566niximlxdzpanmn4qouucvua3k7neniwss47li5r6ugoertzuqzwas",
            "/garlic32/566niximlxdzpanmn4qouucvua3k7neniwss47li5r6ugoertzuqzwassw",
            "/garlic32/566niximlxdzpanmn4qouucvua3k7neniwss47li5r6ugoertzuq/http",
            "/garlic32/566niximlxdzpanmn4qouucvua3k7neniwss47li5r6ugoertzuq/tcp/8080",
            "/garlic32/566niximlxdzpanmn4qouucvua3k7neniwss47li5r6ugoertzuq/udp/8080",
            "/udp/0",
            "/tcp/0",
            "/sctp/0",
            "/udp/1234",
            "/tcp/1234",
            "/sctp/1234",
            "/udp/65535",
            "/tcp/65535",
            "/p2p/QmcgpsyWgH8Y8ajJz1Cu72KnS5uo2Aa2LpzU7kinSupNKC",
            "/p2p/k2k4r8oqamigqdo6o7hsbfwd45y70oyynp98usk7zmyfrzpqxh1pohl7",
            "/p2p/bafzbeigvf25ytwc3akrijfecaotc74udrhcxzh2cx3we5qqnw5vgrei4bm",
            "/p2p/12D3KooWCryG7Mon9orvQxcS1rYZjotPgpwoJNHHKcLLfE4Hf5mV",
            "/p2p/k51qzi5uqu5dhb6l8spkdx7yxafegfkee5by8h7lmjh2ehc2sgg34z7c15vzqs",
            "/p2p/bafzaajaiaejcalj543iwv2d7pkjt7ykvefrkfu7qjfi6sduakhso4lay6abn2d5u",
            "/udp/1234/sctp/1234",
            "/udp/1234/udt",
            "/udp/1234/utp",
            "/tcp/1234/http",
            "/tcp/1234/https",
            "/p2p/QmcgpsyWgH8Y8ajJz1Cu72KnS5uo2Aa2LpzU7kinSupNKC/tcp/1234",
            "/p2p/k2k4r8oqamigqdo6o7hsbfwd45y70oyynp98usk7zmyfrzpqxh1pohl7/tcp/1234",
            "/ip4/127.0.0.1/udp/1234",
            "/ip4/127.0.0.1/udp/0",
            "/ip4/127.0.0.1/tcp/1234",
            "/ip4/127.0.0.1/tcp/1234/",
            "/ip4/127.0.0.1/udp/1234/quic",
            "/ip4/127.0.0.1/udp/1234/quic/webtransport",
            "/ip4/127.0.0.1/udp/1234/quic/webtransport/certhash/b2uaraocy6yrdblb4sfptaddgimjmmpy",
            "/ip4/127.0.0.1/udp/1234/quic/webtransport/certhash/b2uaraocy6yrdblb4sfptaddgimjmmpy/certhash/zQmbWTwYGcmdyK9CYfNBcfs9nhZs17a6FQ4Y8oea278xx41",
            "/ip4/127.0.0.1/p2p/QmcgpsyWgH8Y8ajJz1Cu72KnS5uo2Aa2LpzU7kinSupNKC",
            "/ip4/127.0.0.1/p2p/QmcgpsyWgH8Y8ajJz1Cu72KnS5uo2Aa2LpzU7kinSupNKC/tcp/1234",
            "/ip4/127.0.0.1/p2p/k2k4r8oqamigqdo6o7hsbfwd45y70oyynp98usk7zmyfrzpqxh1pohl7",
            "/ip4/127.0.0.1/p2p/k2k4r8oqamigqdo6o7hsbfwd45y70oyynp98usk7zmyfrzpqxh1pohl7/tcp/1234",
            "/unix/a/b/c/d/e",
            "/unix/stdio",
            "/ip4/1.2.3.4/tcp/80/unix/a/b/c/d/e/f",
            "/ip4/127.0.0.1/p2p/QmcgpsyWgH8Y8ajJz1Cu72KnS5uo2Aa2LpzU7kinSupNKC/tcp/1234/unix/stdio",
            "/ip4/127.0.0.1/p2p/k2k4r8oqamigqdo6o7hsbfwd45y70oyynp98usk7zmyfrzpqxh1pohl7/tcp/1234/unix/stdio",
            "/ip4/127.0.0.1/tcp/9090/http/p2p-webrtc-direct",
            "/ip4/127.0.0.1/tcp/127/ws",
            "/ip4/127.0.0.1/tcp/127/ws",
            "/ip4/127.0.0.1/tcp/127/wss",
            "/ip4/127.0.0.1/tcp/127/wss"
        ).map { multiaddressString ->
            DynamicTest.dynamicTest("Test: $multiaddressString") {
                // We only check here if construction succeeds. 'expectNoErrors' should not trigger
                Multiaddress.fromString(multiaddressString).expectNoErrors()
            }
        }.stream()
    }

    @Test
    fun equality() {
        val m1 = Multiaddress.fromString("/ip4/127.0.0.1/udp/1234").expectNoErrors()
        val m2 = Multiaddress.fromString("/ip4/127.0.0.1/tcp/1234").expectNoErrors()
        val m3 = Multiaddress.fromString("/ip4/127.0.0.1/tcp/1234").expectNoErrors()
        val m4 = Multiaddress.fromString("/ip4/127.0.0.1/tcp/1234/").expectNoErrors()
        assertNotEquals(m1, m2)
        assertNotEquals(m2, m1)
        assertEquals(m2, m3)
        assertEquals(m3, m2)
        assertEquals(m1, m1)
        assertEquals(m2, m4)
        assertEquals(m4, m3)
    }

    @TestFactory
    fun conversion(): Stream<DynamicTest> {
        return listOf(
            Tuple2("/ip4/127.0.0.1/udp/1234", "047f000001910204d2"),
            Tuple2("/ip4/127.0.0.1/tcp/4321", "047f0000010610e1"),
            Tuple2("/ip4/127.0.0.1/udp/1234/ip4/127.0.0.1/tcp/4321", "047f000001910204d2047f0000010610e1"),
            Tuple2("/onion/aaimaq4ygg2iegci:80", "bc030010c0439831b48218480050"),
            Tuple2("/onion3/vww6ybal4bd7szmgncyruucpgfkqahzddi37ktceo3ah7ngmcopnpyyd:1234", "bd03adadec040be047f9658668b11a504f3155001f231a37f54c4476c07fb4cc139ed7e30304d2"),
            Tuple2(
                "/garlic64/jT~IyXaoauTni6N4517EG8mrFUKpy0IlgZh-EY9csMAk82Odatmzr~YTZy8Hv7u~wvkg75EFNOyqb~nAPg-khyp2TS~ObUz8WlqYAM2VlEzJ7wJB91P-cUlKF18zSzVoJFmsrcQHZCirSbWoOknS6iNmsGRh5KVZsBEfp1Dg3gwTipTRIx7Vl5Vy~1OSKQVjYiGZS9q8RL0MF~7xFiKxZDLbPxk0AK9TzGGqm~wMTI2HS0Gm4Ycy8LYPVmLvGonIBYndg2bJC7WLuF6tVjVquiokSVDKFwq70BCUU5AU-EvdOD5KEOAM7mPfw-gJUG4tm1TtvcobrObqoRnmhXPTBTN5H7qDD12AvlwFGnfAlBXjuP4xOUAISL5SRLiulrsMSiT4GcugSI80mF6sdB0zWRgL1yyvoVWeTBn1TqjO27alr95DGTluuSqrNAxgpQzCKEWAyzrQkBfo2avGAmmz2NaHaAvYbOg0QSJz1PLjv2jdPW~ofiQmrGWM1cd~1cCqAAAA",
                "be0383038d3fc8c976a86ae4e78ba378e75ec41bc9ab1542a9cb422581987e118f5cb0c024f3639d6ad9b3aff613672f07bfbbbfc2f920ef910534ecaa6ff9c03e0fa4872a764d2fce6d4cfc5a5a9800cd95944cc9ef0241f753fe71494a175f334b35682459acadc4076428ab49b5a83a49d2ea2366b06461e4a559b0111fa750e0de0c138a94d1231ed5979572ff53922905636221994bdabc44bd0c17fef11622b16432db3f193400af53cc61aa9bfc0c4c8d874b41a6e18732f0b60f5662ef1a89c80589dd8366c90bb58bb85ead56356aba2a244950ca170abbd01094539014f84bdd383e4a10e00cee63dfc3e809506e2d9b54edbdca1bace6eaa119e68573d30533791fba830f5d80be5c051a77c09415e3b8fe3139400848be5244b8ae96bb0c4a24f819cba0488f34985eac741d3359180bd72cafa1559e4c19f54ea8cedbb6a5afde4319396eb92aab340c60a50cc2284580cb3ad09017e8d9abc60269b3d8d687680bd86ce834412273d4f2e3bf68dd3d6fe87e2426ac658cd5c77fd5c0aa000000"
            ),
            Tuple2(
                "/garlic32/566niximlxdzpanmn4qouucvua3k7neniwss47li5r6ugoertzuq",
                "bf0320efbcd45d0c5dc79781ac6f20ea5055a036afb48d45a52e7d68ec7d4338919e69"
            )
        ).map { (multiaddress, binary) ->
            DynamicTest.dynamicTest("Test: $multiaddress") {
                val b1 = Hex.decode(binary).expectNoErrors()
                val b2 = Multiaddress.fromString(multiaddress).expectNoErrors().bytes
                assertArrayEquals(b1, b2, "Failed to convert $multiaddress to ${Hex.encode(b1)} got ${Hex.encode(b2)}")

                val bytes = Hex.decode(binary).expectNoErrors()
                val s2 = Multiaddress.fromBytes(bytes).expectNoErrors().toString()
                assertEquals(multiaddress, s2, "Failed to convert ${Hex.encode(bytes)} to $binary got $s2")
            }
        }.stream()
    }

    @TestFactory
    fun roundtrip(): Stream<DynamicTest> {
        return listOf(
            "/unix/a/b/c/d",
            "/ip6/::ffff:7f00:1/tcp/111",
            "/ip4/127.0.0.1",
            "/ip4/127.0.0.1/tcp/123",
            "/ip4/127.0.0.1/tcp/123/tls",
            "/ip4/127.0.0.1/udp/123",
            "/ip4/127.0.0.1/udp/123/ip6/::",
            "/ip4/127.0.0.1/udp/1234/quic/webtransport/certhash/uEiDDq4_xNyDorZBH3TlGazyJdOWSwvo4PUo5YHFMrvDE8g",
            "/p2p/QmbHVEEepCi7rn7VL7Exxpd2Ci9NNB6ifvqwhsrbRMgQFP",
            "/p2p/QmbHVEEepCi7rn7VL7Exxpd2Ci9NNB6ifvqwhsrbRMgQFP/unix/a/b/c",
            "/ip6/2001:8a0:7ac5:4201:3ac9:86ff:fe31:7095",
            "/ip4/127.0.0.1/tcp/5000",
            "/ip6/2001:8a0:7ac5:4201:3ac9:86ff:fe31:7095/tcp/5000",
            "/ip4/127.0.0.1/udp/5000",
            "/ip6/2001:8a0:7ac5:4201:3ac9:86ff:fe31:7095/udp/5000",
            "/ip4/127.0.0.1/p2p/QmcgpsyWgH8Y8ajJz1Cu72KnS5uo2Aa2LpzU7kinSupNKC/tcp/1234",
            "/ip6/2001:8a0:7ac5:4201:3ac9:86ff:fe31:7095/p2p/QmcgpsyWgH8Y8ajJz1Cu72KnS5uo2Aa2LpzU7kinSupNKC/tcp/1234",
            "/ip4/127.0.0.1/udp/5000/utp",
            "/ip6/2001:8a0:7ac5:4201:3ac9:86ff:fe31:7095/udp/5000/utp",
            "/ip4/127.0.0.1/tcp/8000/http",
            "/ip4/127.0.0.1/tcp/80/unix/a/b/c/d/e/f",
            "/ip6/2001:8a0:7ac5:4201:3ac9:86ff:fe31:7095/tcp/8000/http",
            "/ip6/2001:8a0:7ac5:4201:3ac9:86ff:fe31:7095/tcp/8000/unix/a/b/c/d/e/f",
            "/ip4/127.0.0.1/tcp/8000/https",
            "/ip6/2001:8a0:7ac5:4201:3ac9:86ff:fe31:7095/tcp/8000/https",
            "/ip4/127.0.0.1/tcp/8000/ws",
            "/ip6/2001:8a0:7ac5:4201:3ac9:86ff:fe31:7095/tcp/8000/ws",
            "/ip6/2001:8a0:7ac5:4201:3ac9:86ff:fe31:7095/tcp/8000/ws/p2p/QmcgpsyWgH8Y8ajJz1Cu72KnS5uo2Aa2LpzU7kinSupNKC"
        ).map { multiaddress ->
            DynamicTest.dynamicTest("Test: $multiaddress") {
                val ma = Multiaddress.fromString(multiaddress).expectNoErrors()
                assertEquals(multiaddress, ma.toString())
            }
        }.stream()
    }

    @Test
    fun `ipfs vs p2p`() {
        val p2pAddr = "/p2p/QmbHVEEepCi7rn7VL7Exxpd2Ci9NNB6ifvqwhsrbRMgQFP"
        val ipfsAddr = "/ipfs/QmbHVEEepCi7rn7VL7Exxpd2Ci9NNB6ifvqwhsrbRMgQFP"

        val map2p = Multiaddress.fromString(p2pAddr).expectNoErrors()
        assertEquals(p2pAddr, map2p.toString())

        val maipfs = Multiaddress.fromString(ipfsAddr).expectNoErrors()
        assertEquals(p2pAddr, maipfs.toString())
    }

    @Test
    fun `invalid p2p address bytes`() {
        assertErrorResult("length greater than remaining number of bytes in buffer") { Multiaddress.fromBytes(Hex.decode("a503221221c05877cbae039d70a5e600ea02c6f9f2942439285c9e344e26f8d280c850fad6").expectNoErrors()) }
    }

    @TestFactory
    fun `invalid p2p address string`(): Stream<DynamicTest> {
        val hashedData = Multihash.sum(Multicodec.SHA2_256, "test".toByteArray(), -1).expectNoErrors()
        val unknownCodecCID = CidV1(hashedData, Multicodec.MD5).toString()
        return listOf(
            Tuple2("/p2p/k2k4r8oqamigqdo6o7hsbfwd45y70oyynp98usk7zmyfrzpqxh1pohl-", "failed to parse Multiaddress /p2p/k2k4r8oqamigqdo6o7hsbfwd45y70oyynp98usk7zmyfrzpqxh1pohl-: failed to parse p2p address k2k4r8oqamigqdo6o7hsbfwd45y70oyynp98usk7zmyfrzpqxh1pohl-: invalid base36 digit (-)"), // invalid multibase encoding
            Tuple2("/p2p/?unknownmultibase", "failed to parse Multiaddress /p2p/?unknownmultibase: failed to parse p2p address ?unknownmultibase: Could not determine Multibase from: ?unknownmultibase"), // invalid multibase encoding
            Tuple2("/p2p/k2jmtxwoe2phm1hbqp0e7nufqf6umvuu2e9qd7ana7h411a0haqj6i2z", "failed to parse Multiaddress /p2p/k2jmtxwoe2phm1hbqp0e7nufqf6umvuu2e9qd7ana7h411a0haqj6i2z: failed to parse p2p address: k2jmtxwoe2phm1hbqp0e7nufqf6umvuu2e9qd7ana7h411a0haqj6i2z has an invalid codec dag-pb"), // non-libp2p-key codec
            Tuple2("/p2p/$unknownCodecCID", "failed to parse Multiaddress /p2p/bahkqcerat6dnbamijr6wlgrp5kqmkwwqcwr36ty3fmfyelgrlvwblmhqbiea: failed to parse p2p address: bahkqcerat6dnbamijr6wlgrp5kqmkwwqcwr36ty3fmfyelgrlvwblmhqbiea has an invalid codec md5") // impossible codec
        ).map { (multiaddress, error) ->
            DynamicTest.dynamicTest("Test: $multiaddress") {
                assertErrorResult(error) { Multiaddress.fromString(multiaddress) }
            }
        }.stream()
    }

    @Test
    fun zone() {
        val ip6String = "/ip6zone/eth0/ip6/::1"
        val ip6Bytes = byteArrayOf(
            0x2a, 4,
            'e'.code.toByte(), 't'.code.toByte(), 'h'.code.toByte(), '0'.code.toByte(),
            0x29,
            0, 0, 0, 0,
            0, 0, 0, 0,
            0, 0, 0, 0,
            0, 0, 0, 1
        )
        val ma1 = Multiaddress.fromString(ip6String).expectNoErrors()
        assertArrayEquals(ip6Bytes, ma1.bytes)

        val ma2 = Multiaddress.fromBytes(ip6Bytes).expectNoErrors()
        assertEquals(ip6String, ma2.toString())
    }

    @Test
    fun manipulationBasic() {
        val udpAddrStr = "/ip4/127.0.0.1/udp/1234"
        val udpAddr = Multiaddress.fromString(udpAddrStr).expectNoErrors()
        assertEquals(udpAddrStr, udpAddr.toString())
        assertArrayEquals(udpAddr.bytes, Hex.decode("047f000001910204d2").expectNoErrors())
        assertEquals(udpAddr.protocols().map { obj -> obj.codec.code }, listOf(4, 273))
        assertEquals(udpAddr.protocols().map { obj -> obj.codec.typeName }, listOf("ip4", "udp"))

        val udpAddrbytes2 = udpAddr.encapsulate("/udp/5678").expectNoErrors()
        assertEquals(udpAddrbytes2.toString(), "/ip4/127.0.0.1/udp/1234/udp/5678")
        assertEquals(udpAddrbytes2.decapsulate("/udp").expectNoErrors().toString(), "/ip4/127.0.0.1/udp/1234")
        assertEquals(udpAddrbytes2.decapsulate("/ip4").expectNoErrors().toString(), "/")
        assertErrorResult("failed to parse Multiaddress /ip4/127.0.0.1/udp: unexpected end of Multiaddress") { udpAddr.decapsulate("/") }
        assertEquals(udpAddr.toString(), Multiaddress.fromString("/").expectNoErrors().encapsulate(udpAddr.toString()).expectNoErrors().toString())
        assertEquals("/", Multiaddress.fromString("/").expectNoErrors().decapsulate("/").expectNoErrors().toString())
    }

    @Test
    fun manipulationIpfs() {
        val ipfsAddr = Multiaddress.fromString("/ipfs/QmcgpsyWgH8Y8ajJz1Cu72KnS5uo2Aa2LpzU7kinSupNKC").expectNoErrors()
        val ip6Addr = Multiaddress.fromString("/ip6/2001:8a0:7ac5:4201:3ac9:86ff:fe31:7095").expectNoErrors()
        val tcpAddr = Multiaddress.fromString("/tcp/8000").expectNoErrors()
        val webAddr = Multiaddress.fromString("/ws").expectNoErrors()
        val actual1 = Multiaddress.fromString("/").expectNoErrors()
            .encapsulate(ip6Addr).expectNoErrors()
            .encapsulate(tcpAddr).expectNoErrors()
            .encapsulate(webAddr).expectNoErrors()
            .encapsulate(ipfsAddr).expectNoErrors()
            .toString()
        assertEquals("$ip6Addr$tcpAddr$webAddr$ipfsAddr", actual1)
        val actual2 = Multiaddress.fromString("/").expectNoErrors()
            .encapsulate(ip6Addr).expectNoErrors()
            .encapsulate(tcpAddr).expectNoErrors()
            .encapsulate(webAddr).expectNoErrors()
            .encapsulate(ipfsAddr).expectNoErrors()
            .decapsulate(ipfsAddr).expectNoErrors()
            .toString()
        assertEquals("$ip6Addr$tcpAddr$webAddr", actual2)
        val actual3 = Multiaddress.fromString("/").expectNoErrors()
            .encapsulate(ip6Addr).expectNoErrors()
            .encapsulate(tcpAddr).expectNoErrors()
            .encapsulate(ipfsAddr).expectNoErrors()
            .encapsulate(webAddr).expectNoErrors()
            .decapsulate(webAddr).expectNoErrors()
            .toString()
        assertEquals("$ip6Addr$tcpAddr$ipfsAddr", actual3)
    }
}
