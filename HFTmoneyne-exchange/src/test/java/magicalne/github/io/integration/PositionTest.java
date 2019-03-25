package magicalne.github.io.integration;

import magicalne.github.io.wire.bitmex.Position;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.concurrent.ConcurrentHashMap;

public class PositionTest {
  private static final String SYMBOL = "XBTUSD";
  private Position position;
  private Position newPosition;
  private ConcurrentHashMap<String, Position> positionMap;
  @Before
  public void setUp() {
    position = new Position();
    position.setSymbol(SYMBOL);
    position.setCurrentQty(10);

    newPosition = new Position();
    newPosition.setSymbol(SYMBOL);
    newPosition.setCurrentQty(0);

    positionMap = new ConcurrentHashMap<>();
    positionMap.put(position.getSymbol(), position);
  }

  @Test
  public void copyFromTest() {
    position.copyFrom(newPosition);
    Assert.assertEquals(newPosition.getCurrentQty(), position.getCurrentQty());
  }

  @Test
  public void positionMapTest() {
    Position position = positionMap.get(SYMBOL);
    Assert.assertEquals(this.position.getCurrentQty(), position.getCurrentQty());
    position.copyFrom(newPosition);
    Assert.assertEquals(newPosition.getCurrentQty(), positionMap.get(SYMBOL).getCurrentQty());
  }
}
