import com.github.mustachejava.ObjectHandler;
import com.github.mustachejava.codegen.CodegenObjectHandler;
import com.github.mustachejava.codegen.CodegenReflectionWrapper;
import com.github.mustachejava.indy.IndyObjectHandler;
import com.github.mustachejava.indy.IndyWrapper;
import com.github.mustachejava.reflect.ReflectionObjectHandler;
import com.github.mustachejava.util.Wrapper;
import org.junit.Test;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Method;
import java.util.List;

public class IndyDemo {

  public static final int TIMES = 100000000;

  public static void main(String[] args) throws Throwable {
    IndyDemo indyDemo = new IndyDemo();
    for (int i = 0; i < 10; i++) {
      timeReflectionOH(indyDemo);
      timeCodegenReflectionOH(indyDemo);
      timeIndy(indyDemo);
      timeIndyNoGuard(indyDemo);
      timeIndyOH(indyDemo);
      timeReflection(indyDemo);
      timeReflectionCached(indyDemo);
      timeUnReflection(indyDemo);
      timeUnReflectionCached(indyDemo);
      timeMH(indyDemo);
      timeMHCached(indyDemo);
      timeDirect(indyDemo);
      System.out.println("-----------------");
    }
  }

  public static void timeReflectionOH(IndyDemo indyDemo) throws Throwable {
    long start = System.currentTimeMillis();
    List<Object> scopes = ObjectHandler.makeList(indyDemo);
    for (int i = 0; i < TIMES; i++) {
      REFLECTED.call(scopes);
    }
    System.out.println("reflection OH: " + (System.currentTimeMillis() - start));
  }

  public static void timeCodegenReflectionOH(IndyDemo indyDemo) throws Throwable {
    long start = System.currentTimeMillis();
    List<Object> scopes = ObjectHandler.makeList(indyDemo);
    for (int i = 0; i < TIMES; i++) {
      CODEGEN_REFLECTED.call(scopes);
    }
    System.out.println("codegen reflection OH: " + (System.currentTimeMillis() - start));
  }

  @Test
  public void timeIndyOH() throws Throwable {
    for (int i = 0; i < 10; i++) {
      timeIndy(new IndyDemo());
    }
  }

  @Test
  public void timeReflectionOH() throws Throwable {
    for (int i = 0; i < 10; i++) {
      timeReflectionOH(new IndyDemo());
    }
  }

  public static void timeIndy(IndyDemo indyDemo) throws Throwable {
    long start = System.currentTimeMillis();
    List<Object> scopes = ObjectHandler.makeList(indyDemo);
    for (int i = 0; i < TIMES; i++) {
      INDY.call(scopes);
    }
    System.out.println("indy wrapper: " + (System.currentTimeMillis() - start));
  }

  public static void timeIndyNoGuard(IndyDemo indyDemo) throws Throwable {
    long start = System.currentTimeMillis();
    List<Object> scopes = ObjectHandler.makeList(indyDemo);
    for (int i = 0; i < TIMES; i++) {
      INDY_NOGUARD.call(scopes);
    }
    System.out.println("indy wrapper no guard: " + (System.currentTimeMillis() - start));
  }

  public static void timeIndyOH(IndyDemo indyDemo) throws Throwable {
    long start = System.currentTimeMillis();
    List<Object> scopes = ObjectHandler.makeList(indyDemo);
    for (int i = 0; i < TIMES; i++) {
      INDY_OH.call(scopes);
    }
    System.out.println("indy OH: " + (System.currentTimeMillis() - start));
  }

  public static void timeReflection(IndyDemo indyDemo) throws Throwable {
    long start = System.currentTimeMillis();
    int REFLECTION_TIMES = 10000000;
    for (int i = 0; i < REFLECTION_TIMES; i++) {
      IndyDemo.class.getDeclaredMethod("someMethod").invoke(indyDemo);
    }
    System.out.println("reflection: " + (TIMES/ REFLECTION_TIMES)*(System.currentTimeMillis() - start));
  }

  public static void timeReflectionCached(IndyDemo indyDemo) throws Throwable {
    long start = System.currentTimeMillis();
    Method someMethod = IndyDemo.class.getDeclaredMethod("someMethod");
    for (int i = 0; i < TIMES; i++) {
      someMethod.invoke(indyDemo);
    }
    System.out.println("reflection cached: " + (System.currentTimeMillis() - start));
  }

  public static void timeUnReflection(IndyDemo indyDemo) throws Throwable {
    long start = System.currentTimeMillis();
    int REFLECTION_TIMES = 10000;
    MethodHandles.Lookup lookup = MethodHandles.lookup();
    for (int i = 0; i < REFLECTION_TIMES; i++) {
      int result = (int) lookup.unreflect(IndyDemo.class.getDeclaredMethod("someMethod")).invokeExact(indyDemo);
    }
    System.out.println("unreflection: " + (TIMES/ REFLECTION_TIMES)*(System.currentTimeMillis() - start));
  }

  public static void timeUnReflectionCached(IndyDemo indyDemo) throws Throwable {
    long start = System.currentTimeMillis();
    MethodHandles.Lookup lookup = MethodHandles.lookup();
    MethodHandle someMethod = lookup.unreflect(IndyDemo.class.getDeclaredMethod("someMethod"));
    for (int i = 0; i < TIMES; i++) {
      int result = (int) someMethod.invokeExact(indyDemo);
    }
    System.out.println("unreflection cached: " + (System.currentTimeMillis() - start));
  }

  public static void timeMH(IndyDemo indyDemo) throws Throwable {
    long start = System.currentTimeMillis();
    int REFLECTION_TIMES = 10000;
    MethodHandles.Lookup lookup = MethodHandles.lookup();
    MethodType type = MethodType.methodType(Integer.TYPE);
    for (int i = 0; i < REFLECTION_TIMES; i++) {
      MethodHandle someMethod = lookup.findVirtual(IndyDemo.class, "someMethod", type);
      int result = (int) someMethod.invokeExact(indyDemo);
    }
    System.out.println("methodhandle: " + (TIMES/ REFLECTION_TIMES)*(System.currentTimeMillis() - start));
  }

  public static void timeMHCached(IndyDemo indyDemo) throws Throwable {
    long start = System.currentTimeMillis();
    MethodHandles.Lookup lookup = MethodHandles.lookup();
    MethodType type = MethodType.methodType(Integer.TYPE);
    MethodHandle someMethod = lookup.findVirtual(IndyDemo.class, "someMethod", type);
    for (int i = 0; i < TIMES; i++) {
      int result = (int) someMethod.invokeExact(indyDemo);
    }
    System.out.println("methodhandle cached: " + (System.currentTimeMillis() - start));
  }

  public static void timeDirect(IndyDemo indyDemo) throws Throwable {
    long start = System.currentTimeMillis();
    for (int i = 0; i < TIMES; i++) {
      indyDemo.someMethod();
    }
    System.out.println("direct: " + (System.currentTimeMillis() - start));
  }

  private static Wrapper REFLECTED;
  private static Wrapper INDY;
  private static IndyWrapper INDY_NOGUARD;

  private static Wrapper CODEGEN_REFLECTED;

  private static final Wrapper INDY_OH;

  static {
    IndyDemo indyDemo = new IndyDemo();
    REFLECTED = new ReflectionObjectHandler().find("someMethod", ObjectHandler.makeList(indyDemo));
    CODEGEN_REFLECTED = new CodegenObjectHandler().find("someMethod", ObjectHandler.makeList(indyDemo));
    INDY = IndyWrapper.create((CodegenReflectionWrapper) CODEGEN_REFLECTED);
    INDY_NOGUARD = IndyWrapper.create((CodegenReflectionWrapper) CODEGEN_REFLECTED, false);
    INDY_OH = new IndyObjectHandler().find("someMethod", ObjectHandler.makeList(indyDemo));
  }

  private int length = 0;

  public int someMethod() {
    return length++;
  }
}