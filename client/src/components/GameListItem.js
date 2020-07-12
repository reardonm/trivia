import React from "react";
import PropTypes from "prop-types";

function GameListItem(props) {
  return (
    <tr className="gameListItem">
      <td><button onClick={e => props.onGameSelected(e.target.value)} value={props.id}>Join</button></td>
      <td>{props.name}</td>
      <td>{props.category}</td>
      <td>{props.players}</td>
    </tr>
  );
}

GameListItem.propTypes = {
  id: PropTypes.number.isRequired,
  name: PropTypes.string.isRequired,
  players: PropTypes.number.isRequired,
  onGameSelected: PropTypes.func.isRequired,
  category: PropTypes.string,
};

export default GameListItem;
