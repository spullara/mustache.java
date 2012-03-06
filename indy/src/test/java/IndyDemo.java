import java.lang.invoke.*;
import java.lang.reflect.*;

public class IndyDemo {
  public static void main(String[] args) throws Throwable {
    for (int i = 0; i < 10; i++) {
      timeReflection();
      timeIndy();
    }
  }

  private static final String[] strings;

  static {
    strings = new String[100];
    for (int i = 0; i < 100; i++) {
      strings[i] = "string" + i;
    }
  }

  public static void timeReflection() throws Throwable {
    long start = System.currentTimeMillis();
    for (int i = 0; i < 100000000; i++) {
      REFLECTED.invoke(null, strings[i % 100]);
    }
    System.out.println("reflected: " + (System.currentTimeMillis() - start));
  }

  public static void timeIndy() throws Throwable {
    long start = System.currentTimeMillis();
    for (int i = 0; i < 100000000; i++) {
      HANDLE.invokeExact(strings[i % 100]);
    }
    System.out.println("indy: " + (System.currentTimeMillis() - start));
  }

  private static final Method REFLECTED;
  private static final MethodHandle HANDLE;

  static {
    Method method = null;
    try {
      method = IndyDemo.class.getMethod("someMethod", String.class);
    } catch (Exception e) {}
    REFLECTED = method;

    MethodHandle handle = null;
    try {
      handle = MethodHandles.lookup().unreflect(REFLECTED);
    } catch (Exception e) {}
    HANDLE = handle;
  }

  public static int length = 0;

  public static void someMethod(String a) {
    length = a.length();
  }
}