import React, { Component } from 'react';
import Game from './components/Game';
import Result from './components/Result';
import GameList from './components/GameList';
import questionsApi from './api/Questions';
import categoriesApi from './api/Categories';
import gamesApi from './api/Games';
import './App.css';
import {SERVER_URL} from "./config";

class App extends Component {
  constructor(props) {
    super(props);

    this.state = {
      currentGame: 0,
      newGameName: '',
      newGameCategory: 'All',

      counter: 0,
      questionId: 1,
      question: '',
      answerOptions: [],
      userAnswer: '',
      answersCount: {},
      result: '',
      message: null,
    };

    // hard bind event handlers used in render
    this.handleAnswerSelected = this.handleAnswerSelected.bind(this);
    this.handleGameSelected = this.handleGameSelected.bind(this);
    this.handleNewGameName = this.handleNewGameName.bind(this);
    this.handleNewGameCategory = this.handleNewGameCategory.bind(this);
    this.handleNewGame = this.handleNewGame.bind(this);

  }

  getMessage = e => {
    e.preventDefault();
    fetch(`${SERVER_URL}/api`)
      .then(r => r.text())
      .then(message => this.setState({message: message}))
      .catch(e => console.error(e))
  };

  componentDidMount() {
    this.setState({
      question: questionsApi[0].question,
      answerOptions: questionsApi[0].answers
    });
  }

  handleNewGameName(name){
    this.setState((state) => ({
      newGameName: name,
    }));
  }

  handleNewGameCategory(category){
    this.setState((state) => ({
      newGameCategory: category,
    }));
  }

  handleNewGame(e){
    e.preventDefault();
    this.setState((state) => ({
      currentGame: 999,
    }));
  }

  handleGameSelected(gameId) {
    this.setState((state) => ({
      currentGame: gameId,
    }));
  }

  handleAnswerSelected(event) {
    this.setUserAnswer(event.currentTarget.value);
    if (this.state.questionId < questionsApi.length) {
        setTimeout(() => this.setNextQuestion(), 600);
    } else {
        setTimeout(() => this.setResults(this.getResults()), 600);
    }
  }

  setUserAnswer(answer) {
    this.setState((state) => ({
      answersCount: {
        ...state.answersCount,
        [answer]: (state.answersCount[answer] || 0) + 1
      },
      userAnswer: answer
    }));
  }

  setNextQuestion() {
    const counter = this.state.counter + 1;
    const questionId = this.state.questionId + 1;
    this.setState({
      counter: counter,
      questionId: questionId,
      question: questionsApi[counter].question,
      answerOptions: questionsApi[counter].answers,
      userAnswer: ''
    });
  }

  getResults() {
    const answersCount = this.state.answersCount;
    const answersCountKeys = Object.keys(answersCount);
    const answersCountValues = answersCountKeys.map((key) => answersCount[key]);
    const maxAnswerCount = Math.max.apply(null, answersCountValues);
    return answersCountKeys.filter((key) => answersCount[key] === maxAnswerCount);
  }

  setResults (result) {
    const r = (result.length === 1) ? result[0] : 'Undetermined';
    alert(r);
    this.setState({
      result: '',
      currentGame: 0,
      newGameName: '',
      newGameCategory: 'All',
      question: '',
      counter: 0,
    });
  }

  renderSelectGame() {
    return (
      <GameList
        games={gamesApi}
        categories={categoriesApi}
        newGameName={this.state.newGameName}
        newGameCategory={this.state.newGameCategory}
        onGameSelected={this.handleGameSelected}
        onNewGameName={this.handleNewGameName}
        onNewGameCategory={this.handleNewGameCategory}
        onNewGame={this.handleNewGame}
      />
    );
  }

  renderGame() {
    return (
      <Game
        userAnswer={this.state.userAnswer}
        answerOptions={this.state.answerOptions}
        questionId={this.state.questionId}
        question={this.state.question}
        questionTotal={questionsApi.length}
        onAnswerSelected={this.handleAnswerSelected}
      />
    );
  }

  renderResult() {
    return (
      <Result gameResult={this.state.result} />
    );
  }

  render() {
    return (
      <div className="App">
        <div className="App-header">
          <h2>Trivia</h2>
        </div>

        { this.state.currentGame > 0 ? this.renderGame() : this.renderSelectGame() }

        <p/>
        <div>
          <form onSubmit={this.getMessage}>
            <label>Call server: </label>
            <input type="submit" value="Submit" />
          </form>

          <p>
            {
             this.state.message ?
                <strong>{this.state.message}</strong> :
                <span>Edit <code>src/App.js</code> and save to reload.</span>
            }
          </p>
        </div>
      </div>
    );
  }
}

export default App;
