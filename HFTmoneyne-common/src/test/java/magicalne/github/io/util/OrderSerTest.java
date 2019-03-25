package magicalne.github.io.util;

import magicalne.github.io.wire.bitmex.ActionEnum;
import magicalne.github.io.wire.bitmex.Order;
import magicalne.github.io.wire.bitmex.TableEnum;
import net.openhft.chronicle.bytes.Bytes;
import net.openhft.chronicle.wire.Wire;
import net.openhft.chronicle.wire.WireType;
import org.junit.Assert;
import org.junit.Test;

import java.util.List;

public class OrderSerTest {
  private static final String payload = "{\"table\":\"order\",\"action\":\"partial\",\"keys\":[\"orderID\"],\"types\":{\"orderID\":\"guid\",\"clOrdID\":\"symbol\",\"clOrdLinkID\":\"symbol\",\"account\":\"long\",\"symbol\":\"symbol\",\"side\":\"symbol\",\"simpleOrderQty\":\"float\",\"orderQty\":\"long\",\"price\":\"float\",\"displayQty\":\"long\",\"stopPx\":\"float\",\"pegOffsetValue\":\"float\",\"pegPriceType\":\"symbol\",\"currency\":\"symbol\",\"settlCurrency\":\"symbol\",\"ordType\":\"symbol\",\"timeInForce\":\"symbol\",\"execInst\":\"symbol\",\"contingencyType\":\"symbol\",\"exDestination\":\"symbol\",\"ordStatus\":\"symbol\",\"triggered\":\"symbol\",\"workingIndicator\":\"boolean\",\"ordRejReason\":\"symbol\",\"simpleLeavesQty\":\"float\",\"leavesQty\":\"long\",\"simpleCumQty\":\"float\",\"cumQty\":\"long\",\"avgPx\":\"float\",\"multiLegReportingType\":\"symbol\",\"text\":\"symbol\",\"transactTime\":\"timestamp\",\"timestamp\":\"timestamp\"},\"foreignKeys\":{\"symbol\":\"instrument\",\"side\":\"side\",\"ordStatus\":\"ordStatus\"},\"attributes\":{\"orderID\":\"grouped\",\"account\":\"grouped\",\"ordStatus\":\"grouped\",\"workingIndicator\":\"grouped\"},\"filter\":{\"account\":142243,\"symbol\":\"XBTUSD\"},\"data\":[{\"orderID\":\"e9e88f5d-5fa4-b510-a897-04dfa58110c2\",\"clOrdID\":\"\",\"clOrdLinkID\":\"\",\"account\":142243,\"symbol\":\"XBTUSD\",\"side\":\"Buy\",\"simpleOrderQty\":null,\"orderQty\":20,\"price\":3000,\"displayQty\":null,\"stopPx\":null,\"pegOffsetValue\":null,\"pegPriceType\":\"\",\"currency\":\"USD\",\"settlCurrency\":\"XBt\",\"ordType\":\"Limit\",\"timeInForce\":\"GoodTillCancel\",\"execInst\":\"\",\"contingencyType\":\"\",\"exDestination\":\"XBME\",\"ordStatus\":\"New\",\"triggered\":\"\",\"workingIndicator\":true,\"ordRejReason\":\"\",\"simpleLeavesQty\":null,\"leavesQty\":20,\"simpleCumQty\":null,\"cumQty\":0,\"avgPx\":null,\"multiLegReportingType\":\"SingleSecurity\",\"text\":\"Submission from testnet.bitmex.com\",\"transactTime\":\"2019-03-22T14:38:46.162Z\",\"timestamp\":\"2019-03-22T14:38:46.162Z\"},{\"orderID\":\"c79c481f-83ad-c567-ff35-727158c94be2\",\"clOrdID\":\"\",\"clOrdLinkID\":\"\",\"account\":142243,\"symbol\":\"XBTUSD\",\"side\":\"Buy\",\"simpleOrderQty\":null,\"orderQty\":20,\"price\":2986.5,\"displayQty\":null,\"stopPx\":null,\"pegOffsetValue\":null,\"pegPriceType\":\"\",\"currency\":\"USD\",\"settlCurrency\":\"XBt\",\"ordType\":\"Limit\",\"timeInForce\":\"GoodTillCancel\",\"execInst\":\"\",\"contingencyType\":\"\",\"exDestination\":\"XBME\",\"ordStatus\":\"New\",\"triggered\":\"\",\"workingIndicator\":true,\"ordRejReason\":\"\",\"simpleLeavesQty\":null,\"leavesQty\":20,\"simpleCumQty\":null,\"cumQty\":0,\"avgPx\":null,\"multiLegReportingType\":\"SingleSecurity\",\"text\":\"Submission from testnet.bitmex.com\",\"transactTime\":\"2019-03-23T03:10:54.780Z\",\"timestamp\":\"2019-03-23T03:10:54.780Z\"}]}";

  @Test
  public void test() {
    Wire wire = WireType.JSON.apply(Bytes.fromString(payload));
    TableEnum table = wire.read("table").asEnum(TableEnum.class);
    Assert.assertEquals(TableEnum.order, table);
    ActionEnum action = wire.read("action").asEnum(ActionEnum.class);
    Assert.assertEquals(ActionEnum.partial, action);
    List<Order> orders = wire.read(() -> "data").list(Order.class);
    Assert.assertEquals(20, orders.get(0).getOrderQty());
  }

}
