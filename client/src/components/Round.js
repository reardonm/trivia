import React from 'react';
import PropTypes from 'prop-types';

function Round(props) {
  return (
    <div className="questionCount">
      Round <span>{props.counter}</span> of <span>{props.total}</span>
    </div>
  );
}

Round.propTypes = {
  counter: PropTypes.number.isRequired,
  total: PropTypes.number.isRequired
};

export default Round;
