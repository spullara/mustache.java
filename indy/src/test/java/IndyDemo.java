import com.github.mustachejava.indy.IndyWrapper;
import com.github.mustachejava.reflect.ReflectionObjectHandler;
import com.github.mustachejava.reflect.ReflectionWrapper;
import com.github.mustachejava.util.Wrapper;
import org.junit.Test;

import java.lang.reflect.Method;

public class IndyDemo {

  public static void main(String[] args) throws Throwable {
    IndyDemo indyDemo = new IndyDemo();
    for (int i = 0; i < 10; i++) {
      timeReflectionOH(indyDemo);
      timeIndyOH(indyDemo);
      timeIndyOHNoGuard(indyDemo);
      timeReflection(indyDemo);
      timeReflectionCached(indyDemo);
      timeDirect(indyDemo);
    }
  }

  private static final String[] strings;

  static {
    strings = new String[100];
    for (int i = 0; i < 100; i++) {
      strings[i] = "string" + i;
    }
  }

  public static void timeReflectionOH(IndyDemo indyDemo) throws Throwable {
    long start = System.currentTimeMillis();
    Object[] scopes = {indyDemo};
    for (int i = 0; i < 100000000; i++) {
      REFLECTED.call(scopes);
    }
    System.out.println("reflection OH: " + (System.currentTimeMillis() - start));
  }

  @Test
  public void timeIndyOH() throws Throwable {
    for (int i = 0; i < 10; i++) {
      timeIndyOH(new IndyDemo());
    }
  }

  @Test
  public void timeReflectionOH() throws Throwable {
    for (int i = 0; i < 10; i++) {
      timeReflectionOH(new IndyDemo());
    }
  }

  public static void timeIndyOH(IndyDemo indyDemo) throws Throwable {
    long start = System.currentTimeMillis();
    Object[] scopes = {indyDemo};
    for (int i = 0; i < 100000000; i++) {
      INDY.call(scopes);
    }
    System.out.println("indy OH: " + (System.currentTimeMillis() - start));
  }

  public static void timeIndyOHNoGuard(IndyDemo indyDemo) throws Throwable {
    long start = System.currentTimeMillis();
    Object[] scopes = {indyDemo};
    for (int i = 0; i < 100000000; i++) {
      INDY_NOGUARD.call(scopes);
    }
    System.out.println("indy OH: " + (System.currentTimeMillis() - start));
  }

  public static void timeReflection(IndyDemo indyDemo) throws Throwable {
    long start = System.currentTimeMillis();
    Object[] scopes = {indyDemo};
    for (int i = 0; i < 10000000; i++) {
      IndyDemo.class.getDeclaredMethod("someMethod").invoke(scopes[0]);
    }
    System.out.println("reflection: " + 10*(System.currentTimeMillis() - start));
  }

  public static void timeReflectionCached(IndyDemo indyDemo) throws Throwable {
    long start = System.currentTimeMillis();
    Object[] scopes = {indyDemo};
    Method someMethod = IndyDemo.class.getDeclaredMethod("someMethod");
    for (int i = 0; i < 100000000; i++) {
      someMethod.invoke(scopes[0]);
    }
    System.out.println("reflection cached: " + (System.currentTimeMillis() - start));
  }

  public static void timeDirect(IndyDemo indyDemo) throws Throwable {
    long start = System.currentTimeMillis();
    Object[] scopes = {indyDemo};
    for (int i = 0; i < 100000000; i++) {
      ((IndyDemo)scopes[0]).someMethod();
    }
    System.out.println("direct: " + (System.currentTimeMillis() - start));
  }

  private static Wrapper REFLECTED;
  private static Wrapper INDY;
  private static IndyWrapper INDY_NOGUARD;

  static {
    IndyDemo indyDemo = new IndyDemo();
    REFLECTED = new ReflectionObjectHandler().find("someMethod", new Object[] { indyDemo });
    INDY = IndyWrapper.create((ReflectionWrapper) REFLECTED);
    INDY_NOGUARD = IndyWrapper.create((ReflectionWrapper) REFLECTED, false);
  }

  private int length = 0;

  public int someMethod() {
    return length++;
  }
}