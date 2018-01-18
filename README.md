
## Flexiblelambda
**flexiblelambda** is a Hybris platform extension capable of converting Java 8 lambdas to Flexible search queries.

It uses [JaQue](https://github.com/TrigerSoft/jaque) to parse lambda expressions and build flexible search queries of it.

## Example
For example, the following code:
```
   @Resource
   LambdaFlexibleSearchService lambdaFlexibleSearchService;
    
   public List<OrderModel> getOrdersForUserUid(String uid) {
      final SerializablePredicate<OrderModel> pred = e -> e.getUser().getUid().equals(uid)
            || e.getUser().getName().equals("John");
      final LambdaFlexibleSearchQuery<OrderModel> query = new LambdaFlexibleSearchQuery<>(OrderModel.class)
	         .filter(pred);

      return lambdaFlexibleSearchService.getList(query);
   }
```
 
will execute following flexible search query:
 
```
  SELECT {this.PK} from {Order AS this
  LEFT JOIN User as thisuser on {this.user}={thisuser.PK}} 
  WHERE ({thisuser.uid} = ?a OR {thisuser.name} = ?b)"
```
With value of *uid* and "John" as parameters, allowing you to write readable queries in a type safe way.

See [LambdaFlexibleSearchTranslationServiceImplUnitTest](https://github.com/homik/flexiblelambda/blob/master/testsrc/pl/homik/flexiblelambda/service/impl/LambdaFlexibleSearchTranslationServiceImplUnitTest.java) for more examples and supported cases.
