import com.exametrika.common.utils.Memory;


public class Test {
    public static void main(String[] args) throws Throwable {
        System.out.println(Memory.getMemoryLayout(char[].class, 0));
    }
}
