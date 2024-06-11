import java.util.*;
import java.util.stream.Collectors;

class Main {
    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        int n = scanner.nextInt();
        scanner.nextLine();
        int sum = 0;
        // for (int i = 0; i < n; i++) {
        //     sum += scanner.nextInt();
        // }
        String nums = scanner.nextLine();
        List<Integer> numsList = Arrays.stream(nums.split(" ")).map(Integer::valueOf).collect(Collectors.toList());
        sum = numsList.stream().reduce(Integer::sum).get();
        // for (Integer integer : numsList) {
        //     System.out.printf(integer + " ");
        // }
        // System.out.println();
        // System.out.println(sum);
        List<List<Integer>> ans = new ArrayList<>();
        ArrayList<Integer> arrayList = new ArrayList<>();
        arrayList.add(1);
        arrayList.add(2);
        ans.add(arrayList);
        ans.add(arrayList);
        System.out.println(ans);
    }
}