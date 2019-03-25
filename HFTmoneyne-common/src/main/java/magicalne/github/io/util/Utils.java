package magicalne.github.io.util;

import com.google.common.hash.HashFunction;
import com.google.common.hash.Hashing;

import java.nio.charset.StandardCharsets;
import java.util.LinkedList;
import java.util.List;

public class Utils {
  public static HashFunction bitmexSignatureHashFunction(String apiSecret) {
    return Hashing.hmacSha256(apiSecret.getBytes(StandardCharsets.UTF_8));
  }
  public static String bitmexSignature(String apiSecret, String verb, String path, long expires, String data) {
    HashFunction hashFunction = bitmexSignatureHashFunction(apiSecret);
    String msg = verb + path + expires + data;
    return hashFunction.hashBytes(msg.getBytes(StandardCharsets.UTF_8)).toString();
  }

  public static String extractStringFieldFromJSON(String payload, String fieldName) {
    int payloadLen = payload.length();
    int fieldLen = fieldName.length();
    char[] payloads = payload.toCharArray();
    char[] fieldChars = fieldName.toCharArray();
    int index = 0;
    Index ind = extractStringFiledFromJSONString(index, payloadLen, payloads, fieldLen, fieldChars);
    return payload.substring(ind.start, ind.end);
  }

  public static List<String> extractStringFieldFromJSONArray(String payload, String fieldName) {
    int index = 0;
    List<String> fields = new LinkedList<>();
    int payloadLen = payload.length();
    int fieldLen = fieldName.length();
    char[] payloads = payload.toCharArray();
    char[] fieldChars = fieldName.toCharArray();
    while (index <= payloadLen - 1) {
      Index ind = extractStringFiledFromJSONString(index, payloadLen, payloads, fieldLen, fieldChars);
      index = ind.index;
      if (index < payloadLen) {
        fields.add(payload.substring(ind.start, ind.end));
      }
    }
    return fields;
  }

  public static double volumeBalance(int bidVolume, int askVolume) {
    if (bidVolume == 0 || askVolume == 0) {
      return 0d;
    } else {
      return (bidVolume - askVolume) * 1.0 / (bidVolume + askVolume);
    }
  }

  private static Index extractStringFiledFromJSONString(int index, int payloadLen, char[] payloads,
                                                        int fieldLen, char[] fieldChars) {
    int counter = 0;
    for (; index < payloadLen; index ++) {
      char c = payloads[index];
      if (c == fieldChars[counter]) {
        counter ++;
      } else {
        counter = 0;
      }
      if (fieldLen == counter) {
        break;
      }
    }
    index += 2;
    final char quote = '"';
    int start = 0;
    for (; index < payloadLen; index ++) {
      if (payloads[index] == quote) {
        start = index + 1;
        break;
      }
    }
    int end = 0;
    index ++;
    for (; index < payloadLen; index ++ ) {
      if (payloads[index] == quote) {
        end = index;
        break;
      }
    }
    return new Index(index, start, end);
  }

  private static class Index {
    int index;
    int start;
    int end;

    public Index(int index, int start, int end) {
      this.index = index;
      this.start = start;
      this.end = end;
    }
  }
}
