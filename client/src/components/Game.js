import React from 'react';
import PropTypes from 'prop-types';
import Question from './Question';
import Round from '../components/Round';
import AnswerOption from '../components/AnswerOption';

function Game(props) {

  function renderAnswerOption(content, index) {
    return (
      <AnswerOption
        key={content}
        answerContent={content}
        userAnswer={props.userAnswer}
        answerId={index}
        questionId={props.questionId}
        onAnswerSelected={props.onAnswerSelected}
      />
    );
  }

  return (
      <div className="game">
        <Round
          counter={props.questionId}
          total={props.questionTotal}
        />
        <Question content={props.question} />
        <ul className="answerOptions">
        { props.answerOptions.map((content, index) => renderAnswerOption(content,index)) }
        </ul>
      </div>
  );
}

Game.propTypes = {
  userAnswer: PropTypes.string.isRequired,
  answerOptions: PropTypes.array.isRequired,
  question: PropTypes.string.isRequired,
  questionId: PropTypes.number.isRequired,
  questionTotal: PropTypes.number.isRequired,
  onAnswerSelected: PropTypes.func.isRequired,
};

export default Game;
