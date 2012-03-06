import com.github.mustachejava.indy.IndyWrapper;
import com.github.mustachejava.reflect.ReflectionObjectHandler;
import com.github.mustachejava.reflect.ReflectionWrapper;
import com.github.mustachejava.util.Wrapper;

public class IndyDemo {
  public static void main(String[] args) throws Throwable {
    IndyDemo indyDemo = new IndyDemo();
    REFLECTED = new ReflectionObjectHandler().find("someMethod", new Object[] { indyDemo });
    INDY = IndyWrapper.create((ReflectionWrapper) REFLECTED);
    for (int i = 0; i < 10; i++) {
      timeReflection(indyDemo);
      timeIndy(indyDemo);
    }
  }

  private static final String[] strings;

  static {
    strings = new String[100];
    for (int i = 0; i < 100; i++) {
      strings[i] = "string" + i;
    }
  }

  public static void timeReflection(IndyDemo indyDemo) throws Throwable {
    long start = System.currentTimeMillis();
    Object[] scopes = {indyDemo};
    for (int i = 0; i < 100000000; i++) {
      REFLECTED.call(scopes);
    }
    System.out.println("reflected: " + (System.currentTimeMillis() - start));
  }

  public static void timeIndy(IndyDemo indyDemo) throws Throwable {
    long start = System.currentTimeMillis();
    Object[] scopes = {indyDemo};
    for (int i = 0; i < 100000000; i++) {
      INDY.call(scopes);
    }
    System.out.println("indy: " + (System.currentTimeMillis() - start));
  }

  private static Wrapper REFLECTED;
  private static Wrapper INDY;

  private int length = 0;

  public int someMethod() {
    return length++;
  }
}