package ee.taltech.inbankbackend.service;

import com.github.vladislavgoltjajev.personalcode.locale.estonia.EstonianPersonalCodeValidator;
import ee.taltech.inbankbackend.config.DecisionEngineConstants;
import ee.taltech.inbankbackend.exceptions.*;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.Period;

/**
 * A service class that provides a method for calculating an approved loan amount and period for a customer.
 * The loan amount is calculated based on the customer's credit modifier,
 * which is determined by the last four digits of their ID code.
 */
@Service
public class DecisionEngine {

    // Used to check for the validity of the presented ID code.
    private final EstonianPersonalCodeValidator validator = new EstonianPersonalCodeValidator();
    private int creditModifier = 0;

    /**
     * Calculates the maximum loan amount and period for the customer based on their ID code,
     * the requested loan amount and the loan period.
     * The loan period must be between 12 and 60 months (inclusive).
     * The loan amount must be between 2000 and 10000€ months (inclusive).
     *
     * @param personalCode ID code of the customer that made the request.
     * @param loanAmount Requested loan amount
     * @param loanPeriod Requested loan period
     * @return A Decision object containing the approved loan amount and period, and an error message (if any)
     * @throws InvalidPersonalCodeException If the provided personal ID code is invalid
     * @throws InvalidLoanAmountException If the requested loan amount is invalid
     * @throws InvalidLoanPeriodException If the requested loan period is invalid
     * @throws NoValidLoanException If there is no valid loan found for the given ID code, loan amount and loan period
     */
    public Decision calculateApprovedLoan(String personalCode, Long loanAmount, int loanPeriod)
            throws InvalidPersonalCodeException, InvalidLoanAmountException, InvalidLoanPeriodException,
            NoValidLoanException, InvalidAgeException {
        try {
            verifyInputs(personalCode, loanAmount, loanPeriod);
        } catch (Exception e) {
            return new Decision(null, null, e.getMessage());
        }

        if (!isValidAge(personalCode)) {
            throw new InvalidAgeException("Invalid age!");
        }

        int outputLoanAmount = DecisionEngineConstants.MAXIMUM_LOAN_AMOUNT;
        creditModifier = getCreditModifier(personalCode);

        if (creditModifier == 0) {
            throw new NoValidLoanException("No valid loan found!");
        }

        while (!isLoanApproved(loanPeriod, outputLoanAmount)) {
            outputLoanAmount -= 100;
            //If a suitable loan amount is not found within the selected period
            //the decision engine tries to find a new suitable period.
            if (outputLoanAmount < DecisionEngineConstants.MINIMUM_LOAN_AMOUNT) {
                if (loanPeriod == DecisionEngineConstants.MAXIMUM_LOAN_PERIOD) {
                    throw new NoValidLoanException("No valid loan found!");
                }
                loanPeriod += 1;
                outputLoanAmount = DecisionEngineConstants.MAXIMUM_LOAN_AMOUNT;
            }
        }
        return new Decision(outputLoanAmount, loanPeriod, null);
    }

    /**
     * Checks if the loan would be approved with given loan period and amount.
     * @param loanPeriod period of the loan.
     * @param loanAmount amount of the loan.
     * @return True if approved, false if not approved.
     */
    private boolean isLoanApproved(int loanPeriod, long loanAmount) {
        double creditScore = ((double) creditModifier / loanAmount) * loanPeriod / 10;
        return creditScore >= 0.1;
    }

    /**
     * Calculates the credit modifier of the customer to according to the last four digits of their ID code.
     * Debt - 0000...2499
     * Segment 1 - 2500...4999
     * Segment 2 - 5000...7499
     * Segment 3 - 7500...9999
     *
     * @param personalCode ID code of the customer that made the request.
     * @return Segment to which the customer belongs.
     */
    private int getCreditModifier(String personalCode) {
        int segment = Integer.parseInt(personalCode.substring(personalCode.length() - 4));

        if (segment < 2500) {
            return 0;
        } else if (segment < 5000) {
            return DecisionEngineConstants.SEGMENT_1_CREDIT_MODIFIER;
        } else if (segment < 7500) {
            return DecisionEngineConstants.SEGMENT_2_CREDIT_MODIFIER;
        }

        return DecisionEngineConstants.SEGMENT_3_CREDIT_MODIFIER;
    }

    /**
     * Calculates the age of the customer to according to their ID code. Finally checks if it is valid.
     * @param personalCode ID code of the customer that made the request.
     * @return true, if is valid, false, if is not valid.
     */
    private boolean isValidAge(String personalCode) {
        char firstDigit = personalCode.charAt(0);
        String year = personalCode.substring(1, 3);
        String month = personalCode.substring(3, 5);
        String day = personalCode.substring(5, 7);

        int century = switch (firstDigit) {
            case '1', '2' -> 1800;
            case '3', '4' -> 1900;
            case '5', '6' -> 2000;
            case '7', '8' -> 2100;
            default -> 0;
        };
        int birthYear = century + Integer.parseInt(year);
        LocalDate birthDate = LocalDate.of(birthYear, Integer.parseInt(month), Integer.parseInt(day));
        LocalDate currentDate = LocalDate.now();

        int age = Period.between(birthDate, currentDate).getYears();
        return age >= DecisionEngineConstants.MINIMUM_AGE && age <= DecisionEngineConstants.MAXIMUM_AGE;
    }

    /**
     * Verify that all inputs are valid according to business rules.
     * If inputs are invalid, then throws corresponding exceptions.
     *
     * @param personalCode Provided personal ID code
     * @param loanAmount Requested loan amount
     * @param loanPeriod Requested loan period
     * @throws InvalidPersonalCodeException If the provided personal ID code is invalid
     * @throws InvalidLoanAmountException If the requested loan amount is invalid
     * @throws InvalidLoanPeriodException If the requested loan period is invalid
     */
    private void verifyInputs(String personalCode, Long loanAmount, int loanPeriod)
            throws InvalidPersonalCodeException, InvalidLoanAmountException, InvalidLoanPeriodException {

        if (!validator.isValid(personalCode)) {
            throw new InvalidPersonalCodeException("Invalid personal ID code!");
        }
        if (DecisionEngineConstants.MINIMUM_LOAN_AMOUNT > loanAmount
                || loanAmount > DecisionEngineConstants.MAXIMUM_LOAN_AMOUNT) {
            throw new InvalidLoanAmountException("Invalid loan amount!");
        }
        if (DecisionEngineConstants.MINIMUM_LOAN_PERIOD > loanPeriod
                || loanPeriod > DecisionEngineConstants.MAXIMUM_LOAN_PERIOD) {
            throw new InvalidLoanPeriodException("Invalid loan period!");
        }

    }
}
