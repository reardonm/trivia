import React from 'react';
import PropTypes from 'prop-types';

function AnswerOption(props) {
  return (
    <li className="answerOption">
      <label className="radioCustomLabel">
        <input
          type="radio"
          //className="radioCustomButton"
          name="radioGroup"
          checked={props.answerContent === props.userAnswer}
          //id={props.answerId}
          value={props.answerContent}
          //disabled={props.userAnswer}
          onChange={props.onAnswerSelected}
        />
        {props.answerContent}
      </label>
    </li>
  );
}

AnswerOption.propTypes = {
  answerId: PropTypes.number.isRequired,
  userAnswer: PropTypes.string.isRequired,
  answerContent: PropTypes.string.isRequired,
  onAnswerSelected: PropTypes.func.isRequired,
};

export default AnswerOption;
