import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;
class Rest {}
class Test extends Rest {
  public int num;
  public Test(int num) {
    this.num = num;
  }
  public static void main(String[] args) {
    Set<Test> example = new HashSet<>();
    for (int i = 0; i < 20; i++)
      example.add(new Test(i));
    ArrayList<Test> testlist = new ArrayList<>(example);
    for (int i = 0; i < testlist.size(); i++)
      System.out.println(testlist.get(i).num);
  }
  public boolean equals(Object var1) {
    return ((var1 instanceof Test) && ((Test)var1).num == this.num);
  }
  public int hashCode() {
    if (this.num % 2 == 0) {
      return this.num;
    }
    return 2000000*this.num;
  }
}
