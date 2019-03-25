package magicalne.github.io.util;

import org.junit.Test;

import java.util.List;

import static org.junit.Assert.*;

public class UtilsTest {

  @Test
  public void bitmexSignatureTest1() {
    String apiKey = "LAqUlngMIQkIUjXMUreyu3qn";
    String apiSecret = "chNOOS4KvNXR_Xq4k4c9qsfoKWvnDecLATCRlcBwyKDYnWgO";

    String verb = "GET";
    String path = "/api/v1/instrument";
    long expires = 1518064236;
    String data = "";
    String signature = Utils.bitmexSignature(apiSecret, verb, path, expires, data);
    assertEquals("c7682d435d0cfe87c16098df34ef2eb5a549d4c5a3c2b1f0f77b8af73423bf00", signature);
  }

  @Test
  public void bitmexSignatureTest2() {
    String apiKey = "LAqUlngMIQkIUjXMUreyu3qn";
    String apiSecret = "CqomC-chNOOS4KvNXR_Xq4k4c9qsfoKWvnDecLATCRlcBwyKDYnWgO";

    String verb = "POST";
    String path = "/api/v1/order";
    long expires = 1554117618;
    String data = "ordType=Limit&execInst=ParticipateDoNotInitiate&timeInForce=GoodTillCancel&symbol=XBTUSD&price=3000.0&orderQty=20&side=Buy";
    String signature = Utils.bitmexSignature(apiSecret, verb, path, expires, data);
    assertEquals("ea82512e4a52e0a9d89fa36f9c98cf2eac570c28e7ebac5681e45bda50341544", signature);
  }

  @Test
  public void extractStringFieldFromJSONTest() {
    String payload = "[{\"orderID\":\"e14d5c85-d30d-e896-5419-62b00ce652cd\",\"clOrdID\":\"\",\"clOrdLinkID\":\"\",\"account\":912540,\"symbol\":\"XBTH19\",\"side\":\"Buy\",\"simpleOrderQty\":null,\"orderQty\":10,\"price\":3000,\"displayQty\":null,\"stopPx\":null,\"pegOffsetValue\":null,\"pegPriceType\":\"\",\"currency\":\"USD\",\"settlCurrency\":\"XBt\",\"ordType\":\"Limit\",\"timeInForce\":\"GoodTillCancel\",\"execInst\":\"ParticipateDoNotInitiate\",\"contingencyType\":\"\",\"exDestination\":\"XBME\",\"ordStatus\":\"Canceled\",\"triggered\":\"\",\"workingIndicator\":false,\"ordRejReason\":\"\",\"simpleLeavesQty\":null,\"leavesQty\":0,\"simpleCumQty\":null,\"cumQty\":0,\"avgPx\":null,\"multiLegReportingType\":\"SingleSecurity\",\"text\":\"Canceled: Canceled via API.\\nSubmitted via API.\",\"transactTime\":\"2019-03-08T07:22:54.669Z\",\"timestamp\":\"2019-03-08T07:34:31.763Z\"}]";
    String orderID = Utils.extractStringFieldFromJSON(payload, "orderID");
    assertEquals("e14d5c85-d30d-e896-5419-62b00ce652cd", orderID);
  }

  @Test
  public void extractStringFieldFromJSONArrayTest() {
    String payload = "[{\"orderID\":\"e14d5c85-d30d-e896-5419-62b00ce652cd\",\"clOrdID\":\"\",\"clOrdLinkID\":\"\",\"account\":912540,\"symbol\":\"XBTH19\",\"side\":\"Buy\",\"simpleOrderQty\":null,\"orderQty\":10,\"price\":3000,\"displayQty\":null,\"stopPx\":null,\"pegOffsetValue\":null,\"pegPriceType\":\"\",\"currency\":\"USD\",\"settlCurrency\":\"XBt\",\"ordType\":\"Limit\",\"timeInForce\":\"GoodTillCancel\",\"execInst\":\"ParticipateDoNotInitiate\",\"contingencyType\":\"\",\"exDestination\":\"XBME\",\"ordStatus\":\"Canceled\",\"triggered\":\"\",\"workingIndicator\":false,\"ordRejReason\":\"\",\"simpleLeavesQty\":null,\"leavesQty\":0,\"simpleCumQty\":null,\"cumQty\":0,\"avgPx\":null,\"multiLegReportingType\":\"SingleSecurity\",\"text\":\"Canceled: Canceled via API.\\nSubmitted via API.\",\"transactTime\":\"2019-03-08T07:22:54.669Z\",\"timestamp\":\"2019-03-08T07:34:31.763Z\"},{\"orderID\":\"1e14d5c85-d30d-e896-5419-62b00ce652cd\",\"clOrdID\":\"\",\"clOrdLinkID\":\"\",\"account\":912540,\"symbol\":\"XBTH19\",\"side\":\"Buy\",\"simpleOrderQty\":null,\"orderQty\":10,\"price\":3000,\"displayQty\":null,\"stopPx\":null,\"pegOffsetValue\":null,\"pegPriceType\":\"\",\"currency\":\"USD\",\"settlCurrency\":\"XBt\",\"ordType\":\"Limit\",\"timeInForce\":\"GoodTillCancel\",\"execInst\":\"ParticipateDoNotInitiate\",\"contingencyType\":\"\",\"exDestination\":\"XBME\",\"ordStatus\":\"Canceled\",\"triggered\":\"\",\"workingIndicator\":false,\"ordRejReason\":\"\",\"simpleLeavesQty\":null,\"leavesQty\":0,\"simpleCumQty\":null,\"cumQty\":0,\"avgPx\":null,\"multiLegReportingType\":\"SingleSecurity\",\"text\":\"Canceled: Canceled via API.\\nSubmitted via API.\",\"transactTime\":\"2019-03-08T07:22:54.669Z\",\"timestamp\":\"2019-03-08T07:34:31.763Z\"},{\"orderID\":\"2e14d5c85-d30d-e896-5419-62b00ce652cd\",\"clOrdID\":\"\",\"clOrdLinkID\":\"\",\"account\":912540,\"symbol\":\"XBTH19\",\"side\":\"Buy\",\"simpleOrderQty\":null,\"orderQty\":10,\"price\":3000,\"displayQty\":null,\"stopPx\":null,\"pegOffsetValue\":null,\"pegPriceType\":\"\",\"currency\":\"USD\",\"settlCurrency\":\"XBt\",\"ordType\":\"Limit\",\"timeInForce\":\"GoodTillCancel\",\"execInst\":\"ParticipateDoNotInitiate\",\"contingencyType\":\"\",\"exDestination\":\"XBME\",\"ordStatus\":\"Canceled\",\"triggered\":\"\",\"workingIndicator\":false,\"ordRejReason\":\"\",\"simpleLeavesQty\":null,\"leavesQty\":0,\"simpleCumQty\":null,\"cumQty\":0,\"avgPx\":null,\"multiLegReportingType\":\"SingleSecurity\",\"text\":\"Canceled: Canceled via API.\\nSubmitted via API.\",\"transactTime\":\"2019-03-08T07:22:54.669Z\",\"timestamp\":\"2019-03-08T07:34:31.763Z\"}]";
    List<String> orderIDs = Utils.extractStringFieldFromJSONArray(payload, "orderID");
    assertEquals("e14d5c85-d30d-e896-5419-62b00ce652cd", orderIDs.get(0));
    assertEquals("1e14d5c85-d30d-e896-5419-62b00ce652cd", orderIDs.get(1));
    assertEquals("2e14d5c85-d30d-e896-5419-62b00ce652cd", orderIDs.get(2));
    assertEquals(3, orderIDs.size());
  }

}