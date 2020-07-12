import React from 'react';
import PropTypes from 'prop-types';

function Result(props) {
  return (
    <div className="result">
      RESULT... <strong>{props.gameResult}</strong>
    </div>
  );
}

Result.propTypes = {
  gameResult: PropTypes.string.isRequired,
};

export default Result;
