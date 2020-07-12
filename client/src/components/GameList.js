import React from "react";
import PropTypes from "prop-types";
import GameListItem from "../components/GameListItem";

function GameList(props) {
  return (
    <form onSubmit={e => props.onNewGame(e)}>
    <div>
      <table>
        <tbody>
        <tr><th></th><th>Game Name</th><th>Category</th><th>Player count</th></tr>
        {props.games.map(game => <GameListItem
          key={game.name}
          id={game.id}
          name={game.name}
          category={game.category}
          players={game.players}
          onGameSelected={props.onGameSelected}
          />)}
        <tr>
          <td><input type="submit" disabled={!props.newGameName} value="New Game"/></td>
          <td><input type="text" id="newGameName" name="newGameName" value={props.newGameName} onChange={e => props.onNewGameName(e.target.value)}/></td>
          <td><select id="newGameCategory" name="newGameCategory" value={props.newGameCategory} onChange={e => props.onNewGameCategory(e.target.value)}>
            {props.categories.map(category => <option key={category} value={category}>{category}</option>)}
          </select></td>
          <td></td>
        </tr>
        </tbody>
      </table>
    </div>
    </form>
  );
}

GameList.propTypes = {
  games: PropTypes.array.isRequired,
  categories: PropTypes.array.isRequired,
  onGameSelected: PropTypes.func.isRequired,
  onNewGame: PropTypes.func.isRequired,
  onNewGameName: PropTypes.func.isRequired,
  onNewGameCategory: PropTypes.func.isRequired,
  newGameName: PropTypes.string,
  newGameCategory: PropTypes.string,
};

export default GameList;
