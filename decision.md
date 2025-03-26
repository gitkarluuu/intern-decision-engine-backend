In backend: 
The maximum loan period should be 48 months, but instead it is 60 months.
The calculating of credit score is not even implemented, which is the biggest problem, which I decided to fix.
creditModifier * loanPeriod works, but it was told to use ((credit modifier / loan amount) * loan period) / 10.

It is nice, that the Decision is negative when errorMessage is not null, then we don't need the extra variable.
In verifyInputs functions, we can remove !.

In frontend:
The minimum loan period under the slider is 6 months, but it should be 12 months.
If maximum approved loan amount is bigger than the inserted loan amount, it still shows the inserted load amount,
but it should show the maximum approved loan amount.